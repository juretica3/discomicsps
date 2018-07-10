package discomics.model;

import java.util.List;

public interface TextMinableInput {

    String getMainName();
    List<String> getTextMiningNames(QuerySettings querySettings);

}
