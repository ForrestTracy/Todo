import javax.sound.midi.SysexMessage;
import java.io.*;
import java.text.ParseException;
import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;

/*
TODO Features
- save and re-open a TODO list
    - will need to have initial start menu asking if reopening existing or starting new. For new, ask for title.
- have sub-tasks
- undo last action


REFACTORS
- Dashes needed as its own method
- Static Driver method which instantiated another class
- Todo as its own Class with sub tasks, completion status, etc.
*/


/*
    NOTE - need to manually set up /saved_todos/todo_titles.txt for this program to work
 */

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
        greetingMenu();
        printVisual();
        while (!quit) {
            requestNextAction();
        }
    }

    private static void greetingMenu() {
        boolean engageTopMenu = true;
        while (engageTopMenu) {
            try {
                BufferedReader bufferedReader = new BufferedReader(new FileReader("./saved_todos/todo_titles.txt"));
                StringBuilder stringBuilder = new StringBuilder();
                String line = bufferedReader.readLine();
                while (line != null) {
                    stringBuilder.append(line);
                    stringBuilder.append(System.lineSeparator());
                    line = bufferedReader.readLine();
                }
                String everything = stringBuilder.toString();
                List<String> todos = new ArrayList<>();
                String workingString = "";
                for (int i=0; i < everything.length(); i++) {
                    if (everything.charAt(i) == ',') {
                        todos.add(workingString);
                        workingString = "";
                    } else {
                        workingString += everything.charAt(i);
                    }
                }
                todos.add("Create a new TODO list");
                printTopMenuVisual(todos);
            } catch (FileNotFoundException ex) {
                System.out.println("file not found");
            } catch (IOException ex) {
                System.out.println("There was an error and I need to drill down the right exception");
            }
            engageTopMenu = false;
        }

    }

    private static void printTopMenuVisual(List<String> todos) {
        int dashesNeeded = todos.stream().map(String::length).max(Integer::compare).orElse(1) + 7;
        if (todos.size() >= 10) dashesNeeded += 1;
        String dashes = "";
        for(int i=0; i < dashesNeeded; i++) { dashes += "-"; }
        final String finalDashes = dashes; // here becuase a "final" is needed.
        System.out.println(dashes);
        formatTopMenuLines(todos).forEach(task -> {
            System.out.println(task);
            System.out.println(finalDashes);
        });
        String chosenFileName = null;
        System.out.println("");
        System.out.println("Which file would you like to open?");
        while (chosenFileName == null) {
            chosenFileName = chooseFileToOpen(todos);
            System.out.println(chosenFileName);
        }
//        System.out.println("file: " + chosenFileName);
        openChosenFile(chosenFileName);

    }

    private static String chooseFileToOpen(List<String> todos) {
        try {
            int openIndex = Integer.parseInt(scanner.nextLine());
            if (openIndex < 0 || openIndex > todos.size()) {
                throw new IllegalArgumentException();
            }
            return todos.get(openIndex - 1);
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            System.out.println("Invalid entry. Try again.");
            chooseFileToOpen(todos);
            return null;  // needed?
        }
    }

    // TODO merge this with formatTaskLines()
    private static List<String> formatTopMenuLines(List<String> tasks) {
        List<String> formattedLines = new ArrayList<>();
        final int longest = tasks.stream().map(String::length).max(Integer::compare).orElse(1);
        for (int i=0; i < tasks.size(); i++) {
            // once the task count gets past 10 (double digits), the row gets wider
            String tenSpace = i < 9 && tasks.size() > 9 ? " " : "";
            formattedLines.add("| " + (i+1) + tenSpace + " | " + tasks.get(i) + generateWhiteSpaceEnd(longest, tasks.get(i).length()) + "|");
        }
        return formattedLines;
    }

    private static void openChosenFile(String fileName) {
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader("./saved_todos/" + fileName + ".txt"));
            StringBuilder stringBuilder = new StringBuilder();
            String line = bufferedReader.readLine(); // just need to read the first line
            stringBuilder.append(line);
            String rawTodos = stringBuilder.toString();
            String workingString = "";
            String lastStatus = "";
            String taskOrStatus = "TASK";
            title = fileName;
            for (int i=0; i < rawTodos.length(); i++) {
                if (rawTodos.charAt(i) == ',') {
                    if (taskOrStatus.equals("TASK")) {
                        taskOrStatus = "STATUS";
                        lastStatus = workingString;
                    } else {
                        taskOrStatus = "TASK";
                        addItem(lastStatus, Boolean.valueOf(workingString), tasks.size());
                        lastStatus = "";
                    }
                    workingString = "";
                } else {
                    workingString += rawTodos.charAt(i);
                }
            }
        } catch (FileNotFoundException ex) {
            System.out.println("Caught error in openChosenFile() " + ex);
        } catch (IOException ex) {
            System.out.println("There was an error in openChosenFile() and I need to drill down the right exception");
        }
    }

    private static void writeToFile() {
        String strippedTitle = title.replaceAll("\\s+","_");
        try {
            FileWriter writer = new FileWriter("./saved_todos/" + strippedTitle + ".txt");
            BufferedWriter buffWriter = new BufferedWriter(writer);
            buffWriter.write(singleSaveLineToReopen());  buffWriter.newLine();
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

    private static String singleSaveLineToReopen() {
        String taskString = "";
        for (int i = (tasks.size() - 1); i >= 0; i--) {
            taskString = tasks.get(i) + "," + taskStatus.get(i).toString() + "," + taskString;
        }
        return taskString;
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
        final String finalDashes = dashes; // here becuase a "final" is needed.
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

    private static void addItemDialogue() {
        System.out.println("Enter the new ToDo item: ");
        String newItem = scanner.nextLine();
        if (newItem.equals("cc")) return;
        System.out.println("Position of the new item: ");
        int newPosition = 0;
        try { newPosition = Integer.parseInt(scanner.nextLine()) - 1; }
        catch (Exception e) {} // do nothing. Leave it at index 0
        if (newPosition < 0 || newPosition > tasks.size()) newPosition = tasks.size();
        addItem(newItem, false, newPosition);
    }

    private static void addItem(String newItem, boolean complete, Integer newPosition) {
        if (newPosition == null) {
            newPosition = 0;
        }
        tasks.add(newPosition, newItem);
        taskStatus.add(newPosition, complete);
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

    private static void removeItemDialogue() {
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

    private static void removeItem(int removeIndex) {
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

    private static void moveTask() {
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
            String taskName = tasks.get(fromPosition);
            Boolean status = taskStatus.get(fromPosition);
            if (fromPosition == newPosition) {
                return;
            } else if (fromPosition < newPosition) {
                addItem(taskName, status, newPosition + 1);
                removeItem(fromPosition);
            } else {
                removeItem(fromPosition);
                addItem(taskName, status, newPosition);
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Not a valid entry. Try again.");
            moveTask();
        }
    }

    private static void updateTaskName() {
        System.out.println("Task to rename:");
        try {
            int renamePos = Integer.parseInt(scanner.nextLine()) - 1;
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
        System.out.println("a  = add task       c  = toggle complete");
        System.out.println("r  = remove task    m  = move task");
        System.out.println("t  = change title   u  = update task name");
        System.out.println("pt = populate PT    sd = show recently deleted");
        System.out.println("x  = exit ToDo      cc or < 0 to cancel any action");
        System.out.println("   ");
        System.out.println("What would you like to do next?:  ");
        String nextAction = scanner.nextLine();

        boolean showDeleted = false;

        switch (nextAction) {
            case "x"  : quit = true;           break;
            case "a"  : addItemDialogue();     break;
            case "r"  : removeItemDialogue();  break;
            case "c"  : toggleComplete();      break;
            case "m"  : moveTask();            break;
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
