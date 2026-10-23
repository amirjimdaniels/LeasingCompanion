package com.leasingcompanion.ui;

import com.leasingcompanion.renewal.expiration.LevelingPolicy;
import com.leasingcompanion.renewal.expiration.RenewalPlan;
import com.leasingcompanion.renewal.expiration.RenewalPlanner;
import com.leasingcompanion.renewal.expiration.RenewalSuggestion;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Period;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Function;

/** Lease expiration leveling screen: policy controls, per-day move-out chart and the renewal offer list. */
public final class ExpirationView extends BorderPane {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEE MMM d, yyyy");
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMMM yyyy");

    private final RenewalPlanner planner;
    private final LocalDate today;

    private final Spinner<Integer> maxPerDay = new Spinner<>(1, 20, 3);
    private final Spinner<Integer> daysEarlier = new Spinner<>(0, 30, 14);
    private final Spinner<Integer> daysLater = new Spinner<>(0, 30, 14);
    private final CheckBox keepWithinMonth = new CheckBox("Keep in same month");
    private final CheckBox closedSundays = new CheckBox("No move-outs on Sunday");
    private final Spinner<Integer> windowDays = new Spinner<>(30, 180, 120, 15);
    private final Spinner<Integer> termMonths = new Spinner<>(1, 24, 12);

    private final ComboBox<YearMonth> month = new ComboBox<>();
    private final CategoryAxis dayAxis = new CategoryAxis();
    private final NumberAxis countAxis = new NumberAxis();
    private final BarChart<String, Number> chart = new BarChart<>(dayAxis, countAxis);
    private final TableView<RenewalSuggestion> table = new TableView<>();
    private final Label summary = new Label();

    private RenewalPlan plan;

    public ExpirationView(RenewalPlanner planner, LocalDate today) {
        this.planner = planner;
        this.today = today;

        keepWithinMonth.setSelected(true);
        closedSundays.setSelected(true);
        for (Spinner<Integer> s : Set.of(maxPerDay, daysEarlier, daysLater, windowDays, termMonths)) {
            s.setEditable(true);
            s.setPrefWidth(80);
            s.valueProperty().addListener((obs, o, n) -> recalculate());
        }
        keepWithinMonth.selectedProperty().addListener((obs, o, n) -> recalculate());
        closedSundays.selectedProperty().addListener((obs, o, n) -> recalculate());
        month.valueProperty().addListener((obs, o, n) -> redrawChart());
        month.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(YearMonth ym) {
                return ym == null ? "" : ym.format(MONTH);
            }

            @Override
            public YearMonth fromString(String s) {
                return null;
            }
        });

        setPadding(new Insets(12));
        setTop(buildControls());
        setCenter(buildBody());
        setBottom(summary);
        BorderPane.setMargin(summary, new Insets(8, 0, 0, 0));

        recalculate();
    }

    private VBox buildControls() {
        Label title = new Label("Lease Expiration Leveling");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        Label subtitle = new Label("Renewal candidates are leases ending in the next N days. "
                + "Suggested end dates keep each day at or under the move-out cap.");

        FlowPane policy = new FlowPane(16, 8,
                labeled("Max move-outs / day", maxPerDay),
                labeled("Up to days earlier", daysEarlier),
                labeled("Up to days later", daysLater),
                keepWithinMonth,
                closedSundays);
        FlowPane renewal = new FlowPane(16, 8,
                labeled("Renewal window (days)", windowDays),
                labeled("Default term (months)", termMonths),
                labeled("Chart month", month));

        VBox box = new VBox(8, title, subtitle, policy, renewal);
        box.setPadding(new Insets(0, 0, 12, 0));
        return box;
    }

    private SplitPane buildBody() {
        chart.setAnimated(false);
        chart.setCategoryGap(2);
        chart.setBarGap(0);
        dayAxis.setLabel("Day of month");
        countAxis.setLabel("Lease ends");
        countAxis.setTickUnit(1);
        countAxis.setMinorTickVisible(false);

        table.getColumns().add(column("Unit", s -> s.lease().unit().number()));
        table.getColumns().add(column("Floor plan", s -> s.lease().unit().floorPlan()));
        table.getColumns().add(column("Resident", s -> s.lease().residentName()));
        table.getColumns().add(column("Current end", s -> s.lease().endDate().format(DATE)));
        table.getColumns().add(column("Default renewal end", s -> s.defaultEndDate().format(DATE)));
        table.getColumns().add(column("Suggested end", s -> s.assignment().isScheduled()
                ? s.suggestedEndDate().format(DATE)
                : "No open day: manager review"));
        table.getColumns().add(column("Shift", s -> formatShift(s.assignment().shiftDays())));
        table.getColumns().add(column("Offer term", s -> formatTerm(s.suggestedTerm())));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(RenewalSuggestion item, boolean empty) {
                super.updateItem(item, empty);
                setStyle(item != null && !item.assignment().isScheduled() ? "-fx-background-color: #fde2e1;" : "");
            }
        });

        SplitPane split = new SplitPane(chart, table);
        split.setOrientation(Orientation.VERTICAL);
        split.setDividerPositions(0.45);
        return split;
    }

    private void recalculate() {
        Set<DayOfWeek> closed = closedSundays.isSelected() ? EnumSet.of(DayOfWeek.SUNDAY) : Set.of();
        LevelingPolicy policy = new LevelingPolicy(maxPerDay.getValue(), daysEarlier.getValue(),
                daysLater.getValue(), keepWithinMonth.isSelected(), closed);
        plan = planner.plan(today, windowDays.getValue(), termMonths.getValue(), policy);

        table.setItems(FXCollections.observableArrayList(plan.suggestions()));

        YearMonth selected = month.getValue();
        month.setItems(FXCollections.observableArrayList(plan.affectedMonths()));
        if (selected != null && plan.affectedMonths().contains(selected)) {
            month.setValue(selected);
            redrawChart();
        } else {
            month.setValue(plan.affectedMonths().isEmpty() ? null : plan.affectedMonths().first());
        }

        summary.setText("%d renewal candidates  ·  %d moved off default date  ·  %d need manager review  ·  "
                .formatted(plan.suggestions().size(), plan.movedCount(), plan.needsReviewCount())
                + "days over cap: %d before → %d after".formatted(plan.overloadedDaysBefore(), plan.overloadedDaysAfter()));
    }

    private void redrawChart() {
        chart.getData().clear();
        YearMonth ym = month.getValue();
        if (plan == null || ym == null) {
            return;
        }
        XYChart.Series<String, Number> before = new XYChart.Series<>();
        before.setName("Default terms");
        XYChart.Series<String, Number> after = new XYChart.Series<>();
        after.setName("Suggested terms");
        for (int day = 1; day <= ym.lengthOfMonth(); day++) {
            LocalDate date = ym.atDay(day);
            String label = String.valueOf(day);
            before.getData().add(new XYChart.Data<>(label, plan.loadBefore().getOrDefault(date, 0)));
            after.getData().add(new XYChart.Data<>(label, plan.loadAfter().getOrDefault(date, 0)));
        }
        chart.getData().add(before);
        chart.getData().add(after);
        chart.setTitle("Lease ends per day, %s (cap %d)".formatted(ym.format(MONTH), plan.maxMoveOutsPerDay()));
    }

    private static HBox labeled(String text, javafx.scene.Node control) {
        HBox box = new HBox(6, new Label(text), control);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private static TableColumn<RenewalSuggestion, String> column(String title, Function<RenewalSuggestion, String> value) {
        TableColumn<RenewalSuggestion, String> col = new TableColumn<>(title);
        col.setCellValueFactory(cell -> new SimpleStringProperty(value.apply(cell.getValue())));
        return col;
    }

    private static String formatShift(long days) {
        if (days == 0) {
            return "—";
        }
        return (days > 0 ? "+" : "") + days + " d";
    }

    private static String formatTerm(Period term) {
        int months = (int) term.toTotalMonths();
        return term.getDays() == 0 ? months + " mo" : months + " mo " + term.getDays() + " d";
    }
}
