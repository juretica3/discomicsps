//package discomics.model;
//
//import java.io.Serializable;
//
///**
// * Created by Jure on 21/03/2017.
// */
//public class NetworkNode implements Serializable {
//    static final transient long serialVersionUID = 234657288946256L;
//
//    private String name;
//    private double score;
//
//    NetworkNode(String name, double score) {
//        this.name = name;
//        this.score = score;
//    }
//
//    public String getName() {
//        return name;
//    }
//
//    public double getScore() {
//        return score;
//    }
//
//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (!(o instanceof NetworkNode)) return false;
//
//        NetworkNode that = (NetworkNode) o;
//
//        return name != null ? name.equals(that.name) : that.name == null;
//    }
//
//    @Override
//    public int hashCode() {
//        return name != null ? name.hashCode() : 0;
//    }
//}
