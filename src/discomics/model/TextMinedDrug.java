package discomics.model;

import java.net.SocketException;
import java.util.List;

public class TextMinedDrug extends TextMinedObject<Drug> {


    public TextMinedDrug(Drug textMinableInput, List<CustomInputTermBlock> customSearchTerms, QuerySettings querySettings) {
        super(textMinableInput, customSearchTerms, querySettings);
    }

    public Drug getTextMinableInput() {
        return textMinableInput;
    }


    @Override
    public void queryEPmc() throws SocketException {
        articleCollectableCust.queryEPmc(this.textMinableInput, querySettings);
    }

    @Override
    public void queryPubmed() throws SocketException {
        articleCollectableCust.queryPubmed(this.textMinableInput, querySettings);
    }

    @Override
    public void finalisePostQuery(int maxArticlesRetrieved) {
        articleCollectableCust.finalisePostQuery(maxArticlesRetrieved);
    }
}
