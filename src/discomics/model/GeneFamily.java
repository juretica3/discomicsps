package discomics.model;

import com.sun.istack.internal.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class GeneFamily implements TextMinableInput, Serializable {
    static final transient long serialVersionUID = 3321234414463796L;

    private String familyName;

    public GeneFamily(String familyName) {
        this.familyName = familyName;
    }

    @Override
    public String getMainName() {
        return familyName;
    }

    @Override
    public List<String> getTextMiningNames(@Nullable QuerySettings querySettings) {
        List<String> output = new ArrayList<>();
        output.add(this.familyName.toLowerCase());

        if (this.familyName.charAt(this.familyName.length() - 1) == 's') { // add singular form if family name is in plural form
            output.add(this.familyName.substring(0, this.familyName.length() - 1).toLowerCase());
        }
        return output;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GeneFamily that = (GeneFamily) o;

        return familyName != null ? familyName.equalsIgnoreCase(that.familyName) : that.familyName == null;
    }

    @Override
    public int hashCode() {
        return familyName != null ? familyName.hashCode() : 0;
    }
}
