package discomics.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Created by Jure on 4.9.2016.
 */
public class NetworkEdge implements Serializable {
    static final transient long serialVersionUID = 87642622273256L;

    private String node1;
    private String stringId1;
    private String node2;
    private String stringId2;

    private Float score;
    private Float nscore; // neighbourhood score from inter-gene nucleotide count
    private Float fscore; // fusion score from fused proteins in other species
    private Float pscore; // cooccurrence score from similar absence/presence patterns of genes
    private Float hscore; // homology score, degree of homology of the interactors
    private Float ascore; // coexpression score from similar pattern of mRNA expression
    private Float escore; // experimental score
    private Float dscore; // database score from curated data of various datasets
    private Float tscore; // textmining score (co-occurrence of gene/protein names in abstracts)

    NetworkEdge(String psiMiTabRow) {
        if(psiMiTabRow == null) // every row should contain the word "string"
            return;

        String[] splitPsiMiTabLine = psiMiTabRow.split("\\t");

        try {
            this.stringId1 = splitPsiMiTabLine[0].substring(7);
            this.stringId2 = splitPsiMiTabLine[1].substring(7);
            this.node1 = splitPsiMiTabLine[2];
            this.node2 = splitPsiMiTabLine[3];

            String[] splitScoreField = null;
            if (psiMiTabRow.contains("score")) {
                psiMiTabRow = psiMiTabRow.substring(psiMiTabRow.indexOf("score"), psiMiTabRow.length());
                splitScoreField = psiMiTabRow.split("\\|");
            }

            if (splitScoreField != null) {
                for (String scoreField : splitScoreField) {

                    if (scoreField.contains("nscore")) {
                        String scoreString = scoreField.substring(8, scoreField.length());
                        setNscore(Float.parseFloat(scoreString));
                    } else if (scoreField.contains("fscore")) {
                        String scoreString = scoreField.substring(8, scoreField.length());
                        setFscore(Float.parseFloat(scoreString));
                    } else if (scoreField.contains("pscore")) {
                        String scoreString = scoreField.substring(8, scoreField.length());
                        setPscore(Float.parseFloat(scoreString));
                    } else if (scoreField.contains("hscore")) {
                        String scoreString = scoreField.substring(8, scoreField.length());
                        setHscore(Float.parseFloat(scoreString));
                    } else if (scoreField.contains("ascore")) {
                        String scoreString = scoreField.substring(8, scoreField.length());
                        setAscore(Float.parseFloat(scoreString));
                    } else if (scoreField.contains("escore")) {
                        String scoreString = scoreField.substring(8, scoreField.length());
                        setEscore(Float.parseFloat(scoreString));
                    } else if (scoreField.contains("dscore")) {
                        String scoreString = scoreField.substring(8, scoreField.length());
                        setDscore(Float.parseFloat(scoreString));
                    } else if (scoreField.contains("tscore")) {
                        String scoreString = scoreField.substring(8, scoreField.length());
                        setTscore(Float.parseFloat(scoreString));
                    } else if (scoreField.contains("score")) {
                        String scoreString = scoreField.substring(7, scoreField.length());
                        setScore(Float.parseFloat(scoreString));
                    }
                }
            }

        } catch (StringIndexOutOfBoundsException | ArrayIndexOutOfBoundsException e) {
            e.printStackTrace();
            System.out.println("STR IND OOB EX: " + psiMiTabRow);
        }
    }

    public String getNode1() {
        return node1;
    }

    public String getNode2() {
        return node2;
    }

    public String getStringId1() {
        return stringId1;
    }

    public String getStringId2() {
        return stringId2;
    }

    public Float getScore() {
        return score;
    }

    public Float getNscore() {
        return nscore;
    }

    public Float getFscore() {
        return fscore;
    }

    public Float getPscore() {
        return pscore;
    }

    public Float getHscore() {
        return hscore;
    }

    public Float getAscore() {
        return ascore;
    }

    public Float getEscore() {
        return escore;
    }

    public Float getDscore() {
        return dscore;
    }

    public Float getTscore() {
        return tscore;
    }


    public void setScore(Float score) {
        this.score = score;
    }

    private void setNscore(Float nscore) {
        this.nscore = nscore;
    }

    private void setFscore(Float fscore) {
        this.fscore = fscore;
    }

    private void setPscore(Float pscore) {
        this.pscore = pscore;
    }

    private void setHscore(Float hscore) {
        this.hscore = hscore;
    }

    private void setAscore(Float ascore) {
        this.ascore = ascore;
    }

    private void setEscore(Float escore) {
        this.escore = escore;
    }

    private void setDscore(Float dscore) {
        this.dscore = dscore;
    }

    private void setTscore(Float tscore) {
        this.tscore = tscore;
    }

    // SERIALISATION
    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
    }
}
