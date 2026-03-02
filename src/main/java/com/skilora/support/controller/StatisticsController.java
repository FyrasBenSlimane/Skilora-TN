package com.skilora.support.controller;

import com.skilora.support.entity.SupportTicket;
import com.skilora.support.service.SupportTicketService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.*;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Controller for the statistics view showing ticket distribution charts.
 * Displays a PieChart for status distribution and a BarChart for priority distribution.
 * Ported from branch StatisticsController with full package refactor.
 */
public class StatisticsController implements Initializable {

    @FXML private PieChart statusPieChart;
    @FXML private BarChart<String, Number> priorityBarChart;

    private final SupportTicketService ticketService = SupportTicketService.getInstance();

    /** Callback to close/navigate away */
    private Runnable onClose;

    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadData();
    }

    private void loadData() {
        try {
            List<SupportTicket> tickets = ticketService.findAll();

            // Status Distribution (PieChart)
            Map<String, Long> statusCounts = tickets.stream()
                    .filter(t -> t.getStatus() != null)
                    .collect(Collectors.groupingBy(SupportTicket::getStatus, Collectors.counting()));

            ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList(
                    statusCounts.entrySet().stream()
                            .map(e -> new PieChart.Data(e.getKey(), e.getValue()))
                            .collect(Collectors.toList()));
            statusPieChart.setData(pieChartData);
            statusPieChart.setTitle("Tickets by Status");

            // Priority Distribution (BarChart)
            Map<String, Long> priorityCounts = tickets.stream()
                    .filter(t -> t.getPriority() != null)
                    .collect(Collectors.groupingBy(SupportTicket::getPriority, Collectors.counting()));

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Tickets");
            priorityCounts.forEach((priority, count) ->
                    series.getData().add(new XYChart.Data<>(priority, count)));

            priorityBarChart.getData().add(series);
            priorityBarChart.setTitle("Tickets by Priority");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleClose() {
        if (onClose != null) {
            onClose.run();
        } else {
            javafx.stage.Stage stage = (javafx.stage.Stage) statusPieChart.getScene().getWindow();
            stage.close();
        }
    }
}
