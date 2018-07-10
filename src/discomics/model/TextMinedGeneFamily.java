package discomics.model;

import java.util.List;

public class TextMinedGeneFamily extends TextMinedObject<GeneFamily> {

    public TextMinedGeneFamily(GeneFamily textMinableInput, QuerySettings querySettings) {
        super(textMinableInput, querySettings);
    }

    public TextMinedGeneFamily(GeneFamily textMinableInput, List<CustomInputTermBlock> customSearchTerms, QuerySettings querySettings) {
        super(textMinableInput, customSearchTerms, querySettings);
    }

    public GeneFamily getTextMinableInput() {
        return textMinableInput;
    }

}
