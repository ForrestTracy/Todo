import javax.sound.midi.SysexMessage;
import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;

public class Todo {

    // might be nice to use a Map<String, Boolean> implementation for tasks/taskStatus but then I can't get by index.
    private static List<String> tasks = new ArrayList<>();
    private static List<Boolean> taskStatus = new ArrayList<>();
    private static List<String> recentlyDeleted = new ArrayList<>();
    private static String title = "Default Title";
    private static Scanner scanner = new Scanner(System.in);
    private static boolean quit = false;


    public static void main(String[] args) {
        System.out.println("    ");
        printVisual();
        while (!quit) {
            requestNextAction();
        }
    }

    private static void writeToFile() {
        String strippedTitle = title.replaceAll("\\s+","");
        try {
            FileWriter writer = new FileWriter("./saved_todos/" + strippedTitle + ".txt");
            BufferedWriter buffWriter = new BufferedWriter(writer);
            buffWriter.write("Tasks and Statuses:");      buffWriter.newLine(); buffWriter.newLine();
            buffWriter.write("complete?    task name");   buffWriter.newLine();
            buffWriter.write("_________    _________");   buffWriter.newLine();
            for (int i=0; i < tasks.size(); i++) {
                buffWriter.write(taskStatus.get(i) + "         "  + tasks.get(i));
                buffWriter.newLine();
            }
            buffWriter.write("_______________");  buffWriter.newLine();
            buffWriter.write("Deleted Tasks:");   buffWriter.newLine();
            buffWriter.write("_______________");  buffWriter.newLine();
            for (String deleted : recentlyDeleted) {
                buffWriter.write(deleted);  buffWriter.newLine();
            }
            buffWriter.close();
        } catch (IOException e) { System.out.println("An error occurred writing the file."); }
    }

    private static void printVisual() {
        // next line clears and resets the text to top of the screen on Linux terminals
        System.out.print("\033[H\033[2J");  System.out.flush();  System.out.println();

        if (quit) {
            System.out.println("Closing ToDo program. Final state of the tasks and statuses:");
            System.out.println("  ");
        }

        if (tasks.isEmpty() || tasks.size() == 0) {
            System.out.println("No tasks, yet.");
            return;
        }

        int dashesNeeded = tasks.stream().map(String::length).max(Integer::compare).orElse(1) + 13;
        if (tasks.size() >= 10) dashesNeeded += 1;
        String dashes = "";
        for(int i=0; i < dashesNeeded; i++) { dashes += "-"; }
        final String finalDashes = dashes;
        System.out.println("*** " + title + " ***");
        System.out.println(finalDashes);
        formatTaskLines(tasks).forEach(task -> {
            System.out.println(task);
            System.out.println(finalDashes);
        });
        System.out.println();
    }

    private static void changeTitle() {
        System.out.println("Enter the new title: ");
        title = scanner.nextLine().toUpperCase();
    }

    private static void addItem() {
        System.out.println("Enter the new ToDo item: ");
        String newItem = scanner.nextLine();
        System.out.println("Position of the new item: ");
        int newPosition = 0;
        try { newPosition = Integer.parseInt(scanner.nextLine()) - 1; }
        catch (Exception e) {} // do nothing. Leave it at index 0
        if (newPosition < 0 || newPosition > tasks.size()) newPosition = tasks.size();
        tasks.add(newPosition, newItem);
        taskStatus.add(newPosition, false);
    }

    private static List<String> formatTaskLines(List<String> tasks) {
        List<String> formattedLines = new ArrayList<>();
        final int longest = tasks.stream().map(String::length).max(Integer::compare).orElse(1);
        for (int i=0; i < tasks.size(); i++) {
            // once the task count gets past 10 (double digits), the row gets wider
            String tenSpace = i < 9 && tasks.size() > 9 ? " " : "";
            String x = taskStatus.get(i) == true ? "x" : " ";
            formattedLines.add("| " + (i+1) + tenSpace + " | " + x + " | " + tasks.get(i) + generateWhiteSpaceEnd(longest, tasks.get(i).length()) + "|");
        }
        return formattedLines;
    }

    private static String generateWhiteSpaceEnd(int longest, int current) {
        String whiteSpace = "  ";
        int difference = longest - current;
        for (int i=0; i < difference; i++) {
            whiteSpace += " ";
        }
        return whiteSpace;
    }

    private static void removeItem() {
        System.out.println("Task to remove: ");
        int removeIndex = 0;
        try {
            removeIndex = Integer.parseInt(scanner.nextLine()) - 1;
            if (removeIndex < 0 || removeIndex > tasks.size()) { throw new IllegalArgumentException(); }
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid entry. Try again.");
            removeItem();
            return;
        }
        recentlyDeleted.add(0, tasks.get(removeIndex));
        tasks.remove(removeIndex);
        taskStatus.remove(removeIndex);
    }

    private static void populatePt() {
        List<String> ptList = List.of("90 90 back stretches", "heel raises", "ice", "scraping", "hip tilt things", "one legged squats", "heel raises", "90 90 back stretches", "ice");
        ptList.forEach( item -> {
            tasks.add(item);
            taskStatus.add(false);
        });
    }

    private static void toggleComplete() {
        System.out.println("Task to mark complete");
        int togglePosition = 0;
        try {
            togglePosition = Integer.parseInt(scanner.nextLine()) - 1;
            if (togglePosition < 0 || togglePosition > tasks.size()) { throw new IllegalArgumentException(); }
            taskStatus.set(togglePosition, !taskStatus.get(togglePosition));
        } catch (IllegalArgumentException e) {
            System.out.println("Not a valid entry. Try again.");
            toggleComplete();
        }
    }

    private static void updateTaskName() {
        System.out.println("Task to rename:");
        int renamePos = 0;
        try {
            renamePos = Integer.parseInt(scanner.nextLine()) - 1;
            if (renamePos < 0 || renamePos > tasks.size()) { throw new IllegalArgumentException(); }
            System.out.println("New name:");
            String newName = scanner.nextLine();
            tasks.set(renamePos, newName);
        } catch (IllegalArgumentException e) {
            System.out.println("Not a valid entry. Try again.");
            updateTaskName();
        }
    }

    private static void showRecentlyDeleted() {
        System.out.println("Recently deleted:");
        for (String delItem : recentlyDeleted) {
            System.out.println("-" + delItem);
        }
        System.out.println("--------------");
    }

    private static void requestNextAction() {
        System.out.println("a  = add task");
        System.out.println("c  = toggle complete");
        System.out.println("r  = remove task");
        System.out.println("x  = close ToDo program");
        System.out.println("t  = change title");
        System.out.println("u  = update task name");
        System.out.println("pt = populate PT");
        System.out.println("sd = show recently deleted");
        System.out.println("What would you like to do next?:  ");
        String nextAction = scanner.nextLine();

        boolean showDeleted = false;

        switch (nextAction) {
            case "x"  : quit = true;           break;
            case "a"  : addItem();             break;
            case "r"  : removeItem();          break;
            case "c"  : toggleComplete();      break;
            case "t"  : changeTitle();         break;
            case "u"  : updateTaskName();      break;
            case "pt" : populatePt();          break;
            case "sd" : showDeleted = true;    break;
            default : {
                System.out.println("Not a valid choice");
                return;
            }
        }
        printVisual();
        writeToFile();
        if (showDeleted) { showRecentlyDeleted(); }
    }

}