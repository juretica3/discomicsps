package discomics.model;

import discomics.application.DownloadFileHTTP;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.SocketException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Created by Jure on 26.12.2016.
 */
public class ProteinDrugInteractionNetwork extends ProteinInteractionNetwork {

    private static String STITCH_NETWORK_IMAGE_BASE_URL = "http://stitch.embl.de/api/image/networkList?limit=0&required_score=150&identifiers=";
    private static String STITCH_NETWORK_TAB_DATA_BASE_URL = "http://stitch.embl.de/api/psi-mi-tab/interactionsList?limit=0&required_score=150&identifiers=";

    public ProteinDrugInteractionNetwork() {
        super();
    }

    public ProteinDrugInteractionNetwork(List<Protein> proteins) {
        super(proteins);
    }

    @Override
    public void build() throws SocketException {
        Set<String> allIds = new HashSet<>();
        for (Protein protein : getProteinList()) {
            String stringId = protein.getStringId();

            if (!stringId.isEmpty() && stringId.contains(homoSapiensTaxonId))
                allIds.add(stringId);

            for (Drug interaction : protein.getDrugs()) {
                allIds.add(interaction.getDrugId());
            }
        }

        String urlNoStem = buildQueryURLNoStem(allIds);
        queryTabularInteractionData(STITCH_NETWORK_TAB_DATA_BASE_URL + urlNoStem);
        System.out.println("DRUG INTERACTION NETWORK TAB: " + STITCH_NETWORK_TAB_DATA_BASE_URL + urlNoStem);
        queryNetworkImage(STITCH_NETWORK_IMAGE_BASE_URL + urlNoStem);
        System.out.println("DRUG INTERACTION NETWORK IMG: " + STITCH_NETWORK_IMAGE_BASE_URL + urlNoStem);

        defineNetworkNodes();
    }

    public void build(List<Protein> proteins, List<Drug> drugs) throws SocketException {
        Set<String> proteinIds = new HashSet<>();
        for (Protein protein : proteins) {
            String stringId = protein.getStringId();
            if (stringId != null && stringId.contains(homoSapiensTaxonId))
                proteinIds.add(stringId);
        }
        Set<String> drugIds = new HashSet<>();
        for (Drug drug : drugs) {
            String id = drug.getDrugId();
            if (id != null)
                drugIds.add(id);
        }
        proteinIds.addAll(drugIds);

        String urlNoStem = buildQueryURLNoStem(proteinIds);
        queryNetworkImage(STITCH_NETWORK_IMAGE_BASE_URL + urlNoStem);
        System.out.println("DRUG INTERACTION NETWORK IMG: " + STITCH_NETWORK_IMAGE_BASE_URL + urlNoStem);
        queryTabularInteractionData(STITCH_NETWORK_TAB_DATA_BASE_URL + urlNoStem);
        System.out.println("DRUG INTERACTION NETWORK TAB: " + STITCH_NETWORK_TAB_DATA_BASE_URL + urlNoStem);
    }

    @Override
    void defineNetworkNodes() {
        HashSet<String> networkProteins = new HashSet<>();
        HashSet<String> allNetworkMembers = new HashSet<>();
        List<Protein> proteins = getProteinList();

        this.getProteinProteinInteractionList().forEach(protProtInteraction -> {
            if (!protProtInteraction.getStringId1().contains("CID")) // do not include drugs, only score protein nodes
                networkProteins.add(protProtInteraction.getNode1());
            if (!protProtInteraction.getStringId2().contains("CID"))
                networkProteins.add(protProtInteraction.getNode2());

            allNetworkMembers.add(protProtInteraction.getStringId1());
            allNetworkMembers.add(protProtInteraction.getStringId2());
        });

        networkProteins.forEach(nodeName -> {
            double score = 0; // score is sum of all weights of edges from a particular node
            for (NetworkEdge interaction : getProteinProteinInteractionList()) {
                if (interaction.getNode1().equals(nodeName) || interaction.getNode2().equals(nodeName)) {
                    score += interaction.getScore();
                }
            }

            // search for protein with abbreviation
            for (Protein protein : proteins) {
                if (protein.getMainName().equals(nodeName)) {
                    NetworkNodeProtein networkNode = new NetworkNodeProtein(protein, score, allNetworkMembers.size());
                    networkNodes.add(networkNode);
                    proteins.remove(protein); // once found remove not to search in next iterations (speeds up search)
                    break;
                }
            }
        });
    }

    @Override
    String buildQueryURLNoStem(Set<String> stringIds) {
        StringBuilder sb = new StringBuilder();
        Iterator<String> iterator = stringIds.iterator();

        while (iterator.hasNext()) {
            String id = iterator.next();
            if (id != null) {
                sb.append(id);
                if (iterator.hasNext())
                    sb.append("%0D");
            }
        }
        return sb.toString();
    }

    void queryNetworkImage(String url) throws SocketException {
        super.networkImage = DownloadFileHTTP.downloadOnlineImage(url);
    }

    // SERIALISATION
    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
    }
}
