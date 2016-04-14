import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by floris-jan on 14-04-16.
 */
public class AreaChart1 {
    //Draws the Area Chart-scene
    public static VBox getStat3Scene(int minASCII, int maxASCII) {
        VBox sceneView = new VBox();
        sceneView.setPrefSize(Main.width, Main.height);
        ResultSet results = SQL.getDBResults("jdbc:mysql://127.0.0.1:3306/indice", "root", "root", "select * from diefstal_uit_auto");

        Text sceneText = new Text();
        sceneText.setText("Autodiefstal over verschillende jaren in Rotterdam");
        sceneText.setFont(Font.font("null", FontWeight.MEDIUM, 40));
        sceneText.setWrappingWidth(Main.scene.getWidth());

        CategoryAxis XAxis = new CategoryAxis();
        NumberAxis YAxis = new NumberAxis();
        XAxis.setLabel("Jaar");
        YAxis.setLabel("Cijfer");

        final AreaChart<String, Number> areaChart = new AreaChart(XAxis, YAxis);
        areaChart.setPrefHeight(900);

        //You can simply change the letters in these Strings to change the ranges
        String range1 = "A-J";
        String range2 = "K-O";
        String range3 = "P-Z";
        String range4 = "A-Z";
        Text sceneSubText = new Text();
        sceneSubText.setText("Pick the range you like.");
        ListView<String> pickRangeList = new ListView<>();
        ObservableList<String> items = FXCollections.observableArrayList(range1, range2, range3, range4);
        pickRangeList.setItems(items);

        pickRangeList.getSelectionModel().selectedItemProperty().addListener(
                (ObservableValue<? extends String> ov, String oldValue,
                 String newValue) -> {
                    Main.borderPane.setCenter(getStat3Scene((int) newValue.toLowerCase().charAt(0), (int) newValue.toLowerCase().charAt(2)));
                    pickRangeList.getSelectionModel().select(newValue);
                });

        int number = 0;
        int passed = 0;
        int average = 0;
        String averageYear = "";
        try {
            while (results.next()) {
                String name = results.getString("Gebied");
                char firstLetter = name.toLowerCase().charAt(0);
                int firstASCIILetter = (int) firstLetter;
                number++;
                for(int i = 2; i < 7; i++) {
                    average += results.getInt(i);
                    averageYear = "" + (i + 2004);
                }
                if(firstASCIILetter >= minASCII && firstASCIILetter <= maxASCII) {
                    passed++;
                    XYChart.Series series = new XYChart.Series();
                    for (int i = 2; i < 7; i++) {
                        String year = "" + (i + 2004);
                        try {
                            series.getData().add(new XYChart.Data(year, results.getInt(i)));
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                    series.setName(name);
                    areaChart.getData().add(series);
                }
            }
            average /= number;
            XYChart.Series averageSeries = new XYChart.Series();
            averageSeries.getData().add(new XYChart.Data(averageYear, average));
            averageSeries.setName("Rotterdam Average");
            areaChart.getData().add(averageSeries);
            average = 0;
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        System.out.println("Total number of options: " + number + "\nSelected number of options: " + passed);
        sceneView.getChildren().addAll(sceneText, sceneSubText, pickRangeList, areaChart);
        return sceneView;
    }

}