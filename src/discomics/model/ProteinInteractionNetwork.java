package discomics.model;

import discomics.application.DownloadFileHTTP;
import discomics.application.MyLogger;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import javax.imageio.ImageIO;
import java.io.*;
import java.net.SocketException;
import java.util.*;
import java.util.logging.Level;

/**
 * Created by Jure on 4.9.2016.
 */
public class ProteinInteractionNetwork implements Serializable {
    static final transient long serialVersionUID = 433125273256L;

    private static final String queryStemSTRINGImage = "https://string-db.org/api/image/networkList?";
    private static final String queryStemSTRINGTabular = "https://string-db.org/api/psi-mi-tab/interactions?";

    static final String homoSapiensTaxonId = "9606";

    private List<NetworkEdge> proteinProteinInteractionList;
    private List<Protein> proteinList;

    transient Image networkImage;
    transient List<NetworkNodeProtein> networkNodes;

    public ProteinInteractionNetwork() {
        this.proteinList = new ArrayList<>();
        this.proteinProteinInteractionList = new ArrayList<>();
        this.networkNodes = new ArrayList<>();

        try {
            FileInputStream inputstream = new FileInputStream("resources/discomics/icon/attention-table.png");
            this.networkImage = new Image(inputstream);
        } catch (FileNotFoundException e) {
           e.printStackTrace();
        }
    }

    public ProteinInteractionNetwork(List<Protein> proteins) {
        this();
        this.proteinList = proteins;
    }

    public void build() throws SocketException {
        Set<String> stringIds = new HashSet<>();
        for (Protein protein : this.proteinList) {
            String stringId = protein.getStringId();
            if (stringId != null && stringId.contains(homoSapiensTaxonId))
                stringIds.add(stringId);
        }

        String urlNoStem = buildQueryURLNoStem(stringIds);

        System.out.println("PROT INTERACTION NETWORK TAB: " + queryStemSTRINGTabular + urlNoStem);
        queryTabularInteractionData(queryStemSTRINGTabular + urlNoStem);
        System.out.println("PROT INTERACTION NETWORK IMG: " + queryStemSTRINGImage + urlNoStem);
        queryNetworkImage(queryStemSTRINGImage + urlNoStem);

        defineNetworkNodes();
    }

    void defineNetworkNodes() {
        HashSet<String> networkProteinNames = new HashSet<>();

        this.getProteinProteinInteractionList().forEach(protProtInteraction -> {
            networkProteinNames.add(protProtInteraction.getNode1());
            networkProteinNames.add(protProtInteraction.getNode2());
        });
        int nrNetworkNodes = networkProteinNames.size();

        this.proteinList.forEach(protein -> {
            double score = 0;

            // calculate score for protein of current iteration; if no interactions score stays = 0
            for (NetworkEdge interaction : this.proteinProteinInteractionList) {
                if (interaction.getNode1().equals(protein.getMainName()) || interaction.getNode2().equals(protein.getMainName())) {
                    score += interaction.getScore();
                }
            }
            NetworkNodeProtein node = new NetworkNodeProtein(protein, Math.abs(score), nrNetworkNodes);
            this.networkNodes.add(node);
        });
    }

//    public void clear() {
//        this.networkImage = null;
//        if (proteinProteinInteractionList != null)
//            this.proteinProteinInteractionList.clear();
//    }

    String buildQueryURLNoStem(Set<String> stringIds) {

        StringBuilder sb = new StringBuilder();
        sb.append("limit=0&species=" + homoSapiensTaxonId + "&required_score=150&identifiers=");

        Iterator<String> stringIdIterator = stringIds.iterator();

        while (stringIdIterator.hasNext()) {
            String id = stringIdIterator.next();
            if (id != null) {
                sb.append(id);
                if (stringIdIterator.hasNext())
                    sb.append("%0D");
            }
        }

        return sb.toString();
    }

    void queryNetworkImage(String url) throws SocketException {
        networkImage = DownloadFileHTTP.downloadOnlineImage(url);
    }

    void queryTabularInteractionData(String url) throws SocketException {
        this.proteinProteinInteractionList = new ArrayList<>();

        String psiMiTabFile = DownloadFileHTTP.downloadOnlineFileNoHeader(url);

        boolean searchSuccessful = !psiMiTabFile.contains("Error")
                && !psiMiTabFile.contains("URI Too Long");

        if (searchSuccessful) {
            if (psiMiTabFile.isEmpty()) { // when single protein is passed network is empty
                MyLogger.log(Level.FINE, "No interactions were found");
                return;
            }

            String[] psiMiTabLines = psiMiTabFile.split("\\r?\\n");

            for (String line : psiMiTabLines) {
                NetworkEdge networkEdge = new NetworkEdge(line);
                proteinProteinInteractionList.add(networkEdge);
            }
        }
    }

    public List<NetworkEdge> getProteinProteinInteractionList() {
        return new ArrayList<>(proteinProteinInteractionList);
    }

    public Image getNetworkImage() {
        return networkImage;
    }

    List<Protein> getProteinList() {
        return new ArrayList<>(proteinList);
    }

    public List<String> getStringIdList() {
        Set<String> stringIds = new HashSet<>();
        for (NetworkEdge networkEdge : this.proteinProteinInteractionList) {
            stringIds.add(networkEdge.getStringId1());
            stringIds.add(networkEdge.getStringId2());
        }
        return new ArrayList<>(stringIds);
    }

    public List<NetworkNodeProtein> getNetworkNodes() {
        return new ArrayList<>(networkNodes);
    }

    // SERIALISATION
    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        try {
            networkImage = SwingFXUtils.toFXImage(ImageIO.read(s), null);
        } catch (NullPointerException e) { // image null if network undefined
            networkImage = null;
        }
        this.networkNodes = new ArrayList<>();
        defineNetworkNodes();
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        if (networkImage != null)
            ImageIO.write(SwingFXUtils.fromFXImage(networkImage, null), "png", s);
    }
}
