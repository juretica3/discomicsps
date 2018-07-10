package discomics.model;

import java.io.Serializable;
import java.net.SocketException;
import java.util.List;
/**
 * Created by Jure on 14/11/2016.
 */
public class TextMinedProtein extends TextMinedObject<Protein> implements Serializable {
    private static long serialVersionUID = 35136565465435L;

    public TextMinedProtein(Protein protein, QuerySettings querySettings) throws SocketException {
        super(protein, querySettings);
    }

    public TextMinedProtein(Protein protein, List<CustomInputTermBlock> customSearchTerms, QuerySettings querySettings) {
        super(protein, customSearchTerms, querySettings);
    }


    public void retrievePhysicalProteaseInteractions() throws SocketException {
        if (querySettings.isProteaseSearch())
            getArticleCollectablePlys().retrievePhysicalProteaseInteractions(textMinableInput);
    }

    public Protein getTextMinableInput() {
        if (textMinableInput == null)
            return new Protein();
        else
            return textMinableInput;
    }

    /**
     * ------------------- TABLE GETTERS ---------------------------
     */

    public int getNrDrugInteractions() {
        return textMinableInput.getDrugs().size();
    }

    public double getNetworkScore() {
        if (textMinableInput.getStringId().contains(Protein.getHomoSapiensTaxonId()))
            return textMinableInput.getRawNetworkNode().getScore();
        else
            return 0; // not in the network if protein not human (from rat or mouse)
    }
}
