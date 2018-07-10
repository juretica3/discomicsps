package discomics.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.util.*;

/**
 * Created by Jure on 4.9.2016.
 */
public class ProteinCollection implements Serializable {
    static final transient long serialVersionUID = 6223647358469576L;

    private Set<String> inputProteinSet;
    private Set<Protein> outputProteinList;
    private List<String> proteinsFailedIdentify;

    private ProteinInteractionNetwork proteinInteractionNetwork;

    public ProteinCollection() {
        this.outputProteinList = new HashSet<>();
        this.proteinsFailedIdentify = new ArrayList<>();
        this.inputProteinSet = new HashSet<>();
    }

    public void processUserInput(String userInput) {
        Set<String> output = new HashSet<>();
        userInput = userInput.toUpperCase();
        userInput = userInput.trim();

        // convert userinput to list of genes
        if (!userInput.isEmpty()) {
            String[] temp = userInput.split("\\s+");
            for (int i = 0; i < temp.length; i++)
                temp[i] = temp[i].trim();

            Collections.addAll(output, temp);
        }

        this.inputProteinSet = output;
    }

    public void postprocessProteinList(QuerySettings querySettings) {
        if(querySettings.isSupplementPseudogenes()) { // if protein list is to be supplemented with parental genes of pseudogenes
            List<Protein> supplementedProteins = new ArrayList<>();
            for(Protein protein: this.outputProteinList) { // loop through all user inputted proteins
                String parentGene = protein.convertPseudogeneToParentGene(); // check if gene is pseudogene and retrieve parent gene name
                if (parentGene != null) { // gene is not pseudogene if parentGene is null
                    try {
                        Protein parentProtein = new Protein(parentGene); // create new protein object from parentGene
                        supplementedProteins.add(parentProtein); // add parent gene to protein list

                    } catch (SocketException | MalformedURLException e) {
                        e.printStackTrace();
                    }
                }
            }
            this.outputProteinList.addAll(supplementedProteins);
        }
    }

    public void buildInteractionNetwork() throws SocketException {
        // create and build protein interaction network of only the input proteins
        this.proteinInteractionNetwork = new ProteinInteractionNetwork(new ArrayList<>(this.outputProteinList));
        this.proteinInteractionNetwork.build();

        assignNetworkNodesToProteins();
    }

    private void assignNetworkNodesToProteins() {
        // assign network nodes to proteins in list
        for (Protein protein : outputProteinList) {
            if (protein.getStringId().contains(Protein.getHomoSapiensTaxonId())) { // only assign network node if protein is human; non human proteins excluded from network
                for (NetworkNodeProtein node : proteinInteractionNetwork.getNetworkNodes()) { // search through nodes and assign right one to the protein
                    if (protein.getMainName().equalsIgnoreCase(node.getName())) {
                        protein.setRawNetworkNode(node);
                        break;
                    }
                }
            } else { // if protein is not human set score to 0, since not in network
                protein.setRawNetworkNode(new NetworkNodeProtein(protein, 0, 1));
            }
        }
    }

    public Set<String> getInputProteinSet() {
        return new HashSet<>(inputProteinSet);
    }

    public boolean removeInputProtein(String geneName) {
        return this.inputProteinSet.remove(geneName);
    }

    public List<Protein> getOutputProteinList() {
        return new ArrayList<>(outputProteinList);
    }

    public void addProtein(Protein protein) {
        if (protein != null && protein.isSuccessBuildingNomenclature())
            outputProteinList.add(protein);
        else {
            if (protein != null)
                proteinsFailedIdentify.add(protein.getQueryInputGene());
        }
    }

    public void addProteins(List<Protein> proteins) {
        outputProteinList.addAll(proteins);
    }

    public List<String> getProteinsFailedIdentify() {
        return new ArrayList<>(proteinsFailedIdentify);
    }

    // SERIALISATION
    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
    }
}
