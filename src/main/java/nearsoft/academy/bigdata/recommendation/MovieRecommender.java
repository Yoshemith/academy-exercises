package nearsoft.academy.bigdata.recommendation;

import java.io.*;
import java.util.Hashtable;
import java.util.zip.GZIPInputStream;
import java.util.List;
import java.util.ArrayList;

import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.impl.recommender.GenericUserBasedRecommender;
import org.apache.mahout.cf.taste.impl.similarity.PearsonCorrelationSimilarity;
import org.apache.mahout.cf.taste.impl.neighborhood.ThresholdUserNeighborhood;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.recommender.UserBasedRecommender;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.model.file.FileDataModel;

public class MovieRecommender {
    int totalUsers;
    int totalProducts;
    int totalReviews;
    Hashtable<String, Integer> users;
    Hashtable<String, Integer> productsIndex;
    Hashtable<Integer, String> indexProducts;
    String path;

    DataModel model;
    UserSimilarity similarity;
    UserNeighborhood neighborhood;
    UserBasedRecommender recommender;

    MovieRecommender(String path) throws IOException {
        this.path = path;
        this.totalUsers = 0;
        this.totalProducts = 0;
        this.totalReviews = 0;
        this.users = new Hashtable<String, Integer>();
        this.indexProducts = new Hashtable<Integer, String>();
        this.productsIndex = new Hashtable<String, Integer>();

        try {
            //PROCESSING DATA - FROM GZIP TO TXT TO ITERATE AND BUILD CSV
            String productId = "";
            String score = "";
            String userId = "";

            InputStream file = new FileInputStream(this.path);
            InputStream gzipStream = new GZIPInputStream(file);
            Reader read = new InputStreamReader(gzipStream);
            BufferedReader txtFile = new BufferedReader(read);
            BufferedWriter csvFile = new BufferedWriter(new FileWriter("data/results.csv"));
            String line = txtFile.readLine();

            while (line != null) {
                if (line.contains("product/productId")) {
                    productId = line.split(" ")[1];

                    if (this.productsIndex.get(productId) == null) {
                        this.totalProducts++;
                        this.indexProducts.put(this.totalProducts, productId);
                        this.productsIndex.put(productId, this.totalProducts);
                    }
                } else if (line.contains("review/userId:")) {
                    userId = line.split(" ")[1];

                    if (this.users.get(userId) == null) {
                        this.totalUsers++;
                        this.users.put(userId, this.totalUsers);
                    }
                } else if (line.contains("review/score:")) {
                    score = line.split(" ")[1];
                    this.totalReviews++;
                }
                //if all items have values start writing csvfile
                if ((userId != "") && (productId != "") && (score != "")) {
                    csvFile.write(this.users.get(userId) + "," + this.productsIndex.get(productId) + "," + score + "\n");
                    productId = "";
                    score = "";
                    userId = "";
                }
                line = txtFile.readLine();
            }
            txtFile.close();
            csvFile.close();
            
            //LOADING DATA MODEL
            this.model = new FileDataModel(new File("data/results.csv"));
            this.similarity = new PearsonCorrelationSimilarity(this.model);
            this.neighborhood = new ThresholdUserNeighborhood(0.1, this.similarity, this.model);    
            this.recommender = new GenericUserBasedRecommender(this.model, this.neighborhood, this.similarity);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getTotalReviews() {
        return this.totalReviews;
    }
    
    public int getTotalUsers() {
        return this.totalUsers;
    }
    
    public int getTotalProducts() {
        return this.totalProducts;
    }
    /* Function list of recommendations */
    public List<String> getRecommendationsForUser(String user) throws TasteException  {
        List<String> recommendations = new ArrayList<String>(); 

        for (RecommendedItem recommendation : this.recommender.recommend(users.get(user), 3)) {
            recommendations.add(this.indexProducts.get((int) recommendation.getItemID()));
        }
        return recommendations;
    }
}