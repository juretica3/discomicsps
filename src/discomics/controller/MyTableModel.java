package discomics.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.List;

/**
 * Created by Jure on 9.9.2016.
 */
class MyTableModel<T> {

    private ObservableList<T> data = FXCollections.observableArrayList();
    private TableView<T> tableView;

    MyTableModel(TableView<T> tableView) {
        this.tableView = tableView;
        this.tableView.setItems(this.data);
    }

    void clearAndUpdateTable(List<T> list) {
        if(tableView == null)
            return;

        TableColumn sortColumn = null;
        TableColumn.SortType st = null;

        if (tableView.getSortOrder().size() > 0) {
            sortColumn = tableView.getSortOrder().get(0);
            st = sortColumn.getSortType();
        }

        data.clear();
        data.addAll(list);

        if (sortColumn != null) {
            tableView.getSortOrder().add(sortColumn);
            sortColumn.setSortType(st);
            sortColumn.setSortable(true);
        }
    }

    public ObservableList<T> getData() {
        return data;
    }
}
