package com.lms.analytics.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

/**
 * Utility for navigating between views inside the main content area.
 * The content area StackPane has id="contentArea" set in MainController.
 */
public class NavigationUtil {

    /**
     * Loads an FXML and replaces the content in the #contentArea StackPane.
     * Searches the full scene graph for the node with id="contentArea".
     */
    public static void navigateTo(Node anyNode, String fxmlPath) {
        if (anyNode == null || anyNode.getScene() == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(
                NavigationUtil.class.getResource(fxmlPath));
            Parent newView = loader.load();

            // Try direct scene lookup first
            StackPane contentArea = (StackPane)
                anyNode.getScene().lookup("#contentArea");

            // Fallback: walk up the parent chain to find a StackPane ancestor
            if (contentArea == null) {
                contentArea = findContentArea(anyNode);
            }

            if (contentArea != null) {
                contentArea.getChildren().setAll(newView);
            } else {
                System.err.println("NavigationUtil: #contentArea not found in scene.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Walk up the parent hierarchy to find the StackPane with id="contentArea".
     */
    private static StackPane findContentArea(Node node) {
        javafx.scene.Parent parent = node.getParent();
        while (parent != null) {
            if (parent instanceof StackPane sp && "contentArea".equals(sp.getId())) {
                return sp;
            }
            // Also check children of each parent
            for (Node child : parent.getChildrenUnmodifiable()) {
                if (child instanceof StackPane sp && "contentArea".equals(sp.getId())) {
                    return sp;
                }
            }
            parent = parent.getParent();
        }
        return null;
    }

    /** Navigate back to the admin dashboard */
    public static void backToDashboard(Node anyNode) {
        navigateTo(anyNode, "/fxml/DashboardView.fxml");
    }

    /** Navigate back to the instructor dashboard */
    public static void backToInstructorDashboard(Node anyNode) {
        navigateTo(anyNode, "/fxml/InstructorDashboardView.fxml");
    }

    /** Navigate back to browse courses (student) */
    public static void backToBrowseCourses(Node anyNode) {
        navigateTo(anyNode, "/fxml/BrowseCoursesView.fxml");
    }
}
