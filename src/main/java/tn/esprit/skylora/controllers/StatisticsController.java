package tn.esprit.skylora.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.*;
import javafx.stage.Stage;
import tn.esprit.skylora.entities.Ticket;
import tn.esprit.skylora.services.ServiceTicket;

import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class StatisticsController implements Initializable {

    @FXML
    private PieChart statusPieChart;
    @FXML
    private BarChart<String, Number> priorityBarChart;

    private ServiceTicket serviceTicket = new ServiceTicket();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadData();
    }

    private void loadData() {
        try {
            List<Ticket> tickets = serviceTicket.afficher();

            // Status Distribution (PieChart)
            Map<String, Long> statusCounts = tickets.stream()
                    .collect(Collectors.groupingBy(Ticket::getStatut, Collectors.counting()));

            ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList(
                    statusCounts.entrySet().stream()
                            .map(e -> new PieChart.Data(e.getKey(), e.getValue()))
                            .collect(Collectors.toList()));
            statusPieChart.setData(pieChartData);
            statusPieChart.setTitle("Tickets par Statut");

            // Priority Distribution (BarChart)
            Map<String, Long> priorityCounts = tickets.stream()
                    .collect(Collectors.groupingBy(Ticket::getPriorite, Collectors.counting()));

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Tickets");
            priorityCounts.forEach((priority, count) -> {
                series.getData().add(new XYChart.Data<>(priority, count));
            });

            priorityBarChart.getData().add(series);
            priorityBarChart.setTitle("Tickets par Priorité");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) statusPieChart.getScene().getWindow();
        stage.close();
    }
}
