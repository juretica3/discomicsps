package discomics.model;

import java.io.Serializable;

/**
 * Created by admin on 06/01/2018.
 */
public class GoAnnotation implements Serializable {
    private String goId;
    private String goTerm;
    //String evidence;
    private int type; // 1: biological PProcess, 2: molecular FFunction, 3: cellular CComponent

    GoAnnotation(String goId, String goTerm) {
        this.goId = goId;
        this.goTerm = goTerm.substring(goTerm.indexOf(':') + 1, goTerm.length());
        //this.evidence = evidence;
        if (goTerm.charAt(0) == 'C')
            this.type = 3;
        else if (goTerm.charAt(0) == 'P')
            this.type = 1;
        else if (goTerm.charAt(0) == 'F')
            this.type = 2;
    }

    public String getGoTerm() {
        return goTerm;
    }

    public int getType() {
        return type;
    }

    public String getGoId() {
        return goId;
    }

    @Override
    public String toString() {
        return "GoAnnotation{" +
                "goId='" + goId + '\'' +
                ", goTerm='" + goTerm + '\'' +
                '}';
    }
}
