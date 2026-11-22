
package com.example.taskManagement.dto;

public class TaskDashboardResponse {

    private long totalTasks;
    private long completedTasks;
    private long inProgressTasks;
    private long pendingTasks;
    private long overdueTasks;

    public TaskDashboardResponse(
            long totalTasks,
            long completedTasks,
            long inProgressTasks,
            long pendingTasks,
            long overdueTasks) {

        this.totalTasks = totalTasks;
        this.completedTasks = completedTasks;
        this.inProgressTasks = inProgressTasks;
        this.pendingTasks = pendingTasks;
        this.overdueTasks = overdueTasks;
    }

    public long getTotalTasks() {
        return totalTasks;
    }

    public long getCompletedTasks() {
        return completedTasks;
    }

    public long getInProgressTasks() {
        return inProgressTasks;
    }

    public long getPendingTasks() {
        return pendingTasks;
    }

    public long getOverdueTasks() {
        return overdueTasks;
    }
}
