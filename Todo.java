import java.io.*;
import java.util.Arrays;
import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

/*
TODO Features
- create saved_todos dir if it doesn't exist
- have sub-tasks
- undo last action


REFACTORS
- Dashes needed as its own method
- Todo as its own Class with sub tasks, completion status, etc.
- just one instance of BufferReader?
- openChosenFile() is pretty ugly
*/


/*
    NOTE - need to manually set up /saved_todos/todo_titles.txt for this program to work
 */

public class Todo {

    private static List<Task> tasks = new ArrayList<>();
    private static List<Task> recentlyDeleted = new ArrayList<>();
    private static String title = "Default Title";
    private static Scanner scanner = new Scanner(System.in);
    public boolean quit = false;

    public void greetingMenu() {
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader("./saved_todos/todo_titles.txt"));
            StringBuilder stringBuilder = new StringBuilder();
            String line = bufferedReader.readLine();
            while (line != null) {
                stringBuilder.append(line);
                stringBuilder.append(System.lineSeparator());
                line = bufferedReader.readLine();
            }
            String savedTitles = stringBuilder.toString();
            List<String> todoTitles = Arrays.asList(savedTitles.split("\\s*,\\s*"));
            printOpeningMenuVisual(todoTitles);
        } catch (FileNotFoundException ex) {
            System.out.println("file not found");
        } catch (IOException ex) {
            System.out.println("There was an error and I need to drill down the right exception");
        }
    }

    public void printOpeningMenuVisual(List<String> todoTitles) {
        int dashesNeeded = todoTitles.stream().map(String::length).max(Integer::compare).orElse(1) + 7;
        if (todoTitles.size() >= 10) dashesNeeded += 1;
        String dashes = "";
        for (int i = 0; i < dashesNeeded; i++) {
            dashes += "-";
        }
        final String finalDashes = dashes; // here because a "final" is needed.
        System.out.println(dashes);
        formatTaskLines(todoTitles, false).forEach(task -> {
            System.out.println(task);
            System.out.println(finalDashes);
        });
        System.out.println("");
        System.out.println("Which file would you like to open?");
        String chosenFileName = null;
        while (chosenFileName == null) {
            chosenFileName = chooseFileToOpen(todoTitles);
        }
        openChosenFile(chosenFileName);
    }

    public String chooseFileToOpen(List<String> todoTitles) {
        try {
            int openIndex = Integer.parseInt(scanner.nextLine());
            if (openIndex < 0 || openIndex > todoTitles.size()) {
                throw new IllegalArgumentException();
            }
            return todoTitles.get(openIndex - 1);
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            System.out.println("Invalid entry. Try again.");
            return chooseFileToOpen(todoTitles);
        }
    }

    public void openChosenFile(String fileName) {
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader("./saved_todos/" + fileName + ".txt"));
            StringBuilder stringBuilder = new StringBuilder();
            String savedInfoOnFirstLine = bufferedReader.readLine(); // just need to read the first line
            stringBuilder.append(savedInfoOnFirstLine);
            String rawSavedTasks = stringBuilder.toString();
            String workingString = "";
            String lastStatus = "";
            String taskOrStatus = "TASK";
            title = fileName;
            for (int i = 0; i < rawSavedTasks.length(); i++) {
                if (rawSavedTasks.charAt(i) == ',') {
                    if (taskOrStatus.equals("TASK")) {
                        taskOrStatus = "STATUS";
                        lastStatus = workingString;
                    } else {
                        taskOrStatus = "TASK";
                        addTask(lastStatus, Boolean.valueOf(workingString), tasks.size());
                        lastStatus = "";
                    }
                    workingString = "";
                } else {
                    workingString += rawSavedTasks.charAt(i);
                }
            }
        } catch (FileNotFoundException ex) {
            System.out.println("Caught error in openChosenFile() " + ex);
        } catch (IOException ex) {
            System.out.println("There was an error in openChosenFile() and I need to drill down the right exception");
        }
    }

    public void writeToFile() {
        try {
            String strippedTitle = title.strip().replaceAll("\\s+", "_");
            FileWriter writer = new FileWriter("./saved_todos/" + strippedTitle + ".txt");
            BufferedWriter buffWriter = new BufferedWriter(writer);
            StringBuilder taskString = new StringBuilder();
            for (int i = (tasks.size() - 1); i >= 0; i--) {
                taskString.insert(0, tasks.get(i).name + "," + tasks.get(i).status.toString() + ",");
            }
            buffWriter.write(taskString.toString());
            for (Task deletedTask : recentlyDeleted) {
                buffWriter.write(deletedTask.getName());
                buffWriter.newLine();
            }
            buffWriter.close();
        } catch (IOException e) {
            System.out.println("An error occurred writing the file.");
        }
    }

    public void printVisual() {
        // next line clears and resets the text to top of the screen on Linux terminals
        System.out.print("\033[H\033[2J");
        System.out.flush();
        System.out.println();

        if (quit) {
            System.out.println("Closing ToDo program. Final state of the tasks and statuses:");
            System.out.println("  ");
        }

        if (tasks.isEmpty()) {
            System.out.println("No tasks, yet.");
            return;
        }

        int dashesNeeded = tasks.stream()
                .map(task -> task.getName().length())
                .max(Integer::compare)
                .orElse(1) + 13;

        if (tasks.size() >= 10) dashesNeeded += 1;
        StringBuilder dashes = new StringBuilder();
        dashes.append("-".repeat(dashesNeeded));
        final String finalDashes = dashes.toString(); // here becuase a "final" is needed.
        System.out.println("*** " + title + " ***");
        System.out.println(finalDashes);
        formatTaskLines(null, true).forEach(task -> {
            System.out.println(task);
            System.out.println(finalDashes);
        });
        System.out.println();
    }

    public void changeTitle() {
        System.out.println("Enter the new title: ");
        title = scanner.nextLine().toUpperCase();
    }

    public void addItemDialogue() {
        System.out.println("Enter the new ToDo item: ");
        String newItem = scanner.nextLine();
        if (newItem.equals("cc")) return;
        System.out.println("Position of the new item: ");
        int newPosition = 0;
        try {
            newPosition = Integer.parseInt(scanner.nextLine()) - 1;
        } catch (Exception e) {
            // do nothing. Leave it at index 0
        }
        if (newPosition < 0 || newPosition > tasks.size()) newPosition = tasks.size();
        addTask(newItem, false, newPosition);
    }

    public void addTask(String name, boolean status, Integer newPosition) {
        if (newPosition == null) {
            newPosition = 0;
        }
        tasks.add(newPosition, new Task(name, status));
    }

    public List<String> formatTaskLines(List<String> listToFormat, boolean statusNeeded) {
        if (listToFormat == null) {
            listToFormat = tasks.stream()
                    .map(Task::getName)
                    .collect(Collectors.toList());
        }
        List<String> formattedLines = new ArrayList<>();
        final int longest = listToFormat.stream()
                .map(String::length)
                .max(Integer::compare)
                .orElse(1);
        for (int i = 0; i < listToFormat.size(); i++) {
            // once the task count gets past 10 (double digits), the row gets wider
            String tenSpace = i < 9 && listToFormat.size() > 9 ? " " : "";
            String status = "|   ";
            if (statusNeeded) {
                status = tasks.get(i).status ? "x" : " ";
                status = "| " + (i + 1) + tenSpace + " | " + status + " | ";
            }
            formattedLines.add(status + listToFormat.get(i) + generateWhiteSpaceEnd(longest, listToFormat.get(i).length()) + "|");
        }
        return formattedLines;
    }

    public String generateWhiteSpaceEnd(int longest, int current) {
        return " ".repeat(longest - current + 2);
    }

    public void removeItemDialogue() {
        System.out.println("Task to remove: ");
        int removeIndex = 0;
        try {
            removeIndex = Integer.parseInt(scanner.nextLine()) - 1;
            if (removeIndex < 0) {
                return;
            } else if (removeIndex > tasks.size()) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid entry. Try again.");
            removeItemDialogue();
            return;
        }
        removeItem(removeIndex);
    }

    public void removeItem(int removeIndex) {
        recentlyDeleted.add(0, tasks.get(removeIndex));
        tasks.remove(removeIndex);
    }

    public void populatePt() {
        List<String> ptList = List.of("90 90 back stretches", "heel raises", "ice", "scraping", "hip tilt things", "one legged squats", "heel raises", "90 90 back stretches", "ice");
        ptList.forEach(itemName -> {
            tasks.add(new Task(itemName, false));
        });
    }

    public void toggleComplete() {
        System.out.println("Task to mark complete");
        int togglePosition = 0;
        try {
            togglePosition = Integer.parseInt(scanner.nextLine()) - 1;
            if (togglePosition < 0 || togglePosition > tasks.size()) {
                throw new IllegalArgumentException();
            }
            tasks.get(togglePosition).setStatus(!tasks.get(togglePosition).getStatus());
        } catch (IllegalArgumentException e) {
            System.out.println("Not a valid entry. Try again.");
            toggleComplete();
        }
    }

    public void moveTask() {
        System.out.println("Which task do you want to move?");
        try {
            int fromPosition = Integer.parseInt(scanner.nextLine()) - 1;
            if (fromPosition < 0) {
                return;
            } else if (fromPosition > tasks.size()) {
                throw new IllegalArgumentException();
            }
            System.out.println("New placement?");
            int newPosition = Integer.parseInt(scanner.nextLine()) - 1;
            if (newPosition < 0) {
                return;
            } else if (newPosition > tasks.size()) {
                throw new IllegalArgumentException();
            }
            Task task = tasks.get(fromPosition);
            if (fromPosition == newPosition) {
                return;
            } else if (fromPosition < newPosition) {
                addTask(task.getName(), task.getStatus(), newPosition + 1);
                removeItem(fromPosition);
            } else {
                removeItem(fromPosition);
                addTask(task.getName(), task.getStatus(), newPosition);
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Not a valid entry. Try again.");
            moveTask();
        }
    }

    public void updateTaskName() {
        System.out.println("Task to rename:");
        try {
            int renamePos = Integer.parseInt(scanner.nextLine()) - 1;
            if (renamePos < 0 || renamePos > tasks.size()) {
                throw new IllegalArgumentException();
            }
            System.out.println("New name:");
            tasks.get(renamePos).setName(scanner.nextLine());
        } catch (IllegalArgumentException e) {
            System.out.println("Not a valid entry. Try again.");
            updateTaskName();
        }
    }

    public void showRecentlyDeleted() {
        System.out.println("Recently deleted:");
        for (Task deletedTask : recentlyDeleted) {
            System.out.println("-" + deletedTask.getName());
        }
        System.out.println("--------------");
    }

    public void requestNextAction(boolean showFullMenu) {
        if (showFullMenu) {
            System.out.println("a  = add task       c  = toggle complete");
            System.out.println("r  = remove task    m  = move task");
            System.out.println("t  = change title   u  = update task name");
            System.out.println("pt = populate PT    sd = show recently deleted");
            System.out.println("x  = exit ToDo      cc or < 0 to cancel any action");
            System.out.println("   ");
            System.out.println("What would you like to do next?:  ");
        }

        String nextAction = scanner.nextLine();
        boolean showDeleted = false;
        switch (nextAction) {
            case "x" -> quit = true;
            case "a" -> addItemDialogue();
            case "r" -> removeItemDialogue();
            case "c" -> toggleComplete();
            case "m" -> moveTask();
            case "t" -> changeTitle();
            case "u" -> updateTaskName();
            case "pt" -> populatePt();
            case "sd" -> showDeleted = true;
            default -> {
                System.out.println("Not a valid choice");
                requestNextAction(false); // this causes an extra printVisual() because there is no return but a return would recall requestNextAction(true)
            }
        }
        printVisual();
        writeToFile();
        if (showDeleted) {
            showRecentlyDeleted();
        }
    }

    private static class Task {

        public Task(String name, Boolean status) {
            setName(name);
            setStatus(status);
        }

        private String name;
        private Boolean status;

        public String getName() {
            return name;
        }

        public void setName(String newName) {
            this.name = newName;
        }

        public Boolean getStatus() {
            return status;
        }

        public void setStatus(Boolean newStatus) {
            this.status = newStatus;
        }

    }

}
