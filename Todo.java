import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/*

TODO Known Bugs
-


TODO Features
- WIP Shortcut commands "d1" in stead of "delete enter 1"
    - This needs refactoring of the methods it would call
    pull out dialogue, validation of todos index chosen, and execution of action
    currently, they're all in one method
- sort completed tasks to bottom
- have sub-tasks
- Track days since task was created
- ability to remove a TODO list from printOpeningMenuVisual


TODO REFACTORS
- just one instance of BufferReader?
- openChosenFile() is pretty ugly
- Make it DRY-er. Pull out common code from the *Dialogue() methods into one method
- Unify validations
*/

public class Todo {

    private static List<Task> tasks = new ArrayList<>();
    private static List<Task> recentlyDeleted = new ArrayList<>();
    private static String title = "Default Title";
    private static Scanner scanner = new Scanner(System.in);
    public boolean quit = false;
    public boolean showFullMenu = false;

    public void greetingMenu() {
        String savedTitles = getGreetingMenuTitleString();
        List<String> todoTitles = Arrays.asList(savedTitles.split("\\s*,\\s*"));
        printOpeningMenuVisual(todoTitles);
    }

    public String getGreetingMenuTitleString() {
        try {
            createSaveDirStructure();
            BufferedReader bufferedReader = new BufferedReader(new FileReader("./saved_todos/todo_titles.txt"));
            StringBuilder stringBuilder = new StringBuilder();
            String line = bufferedReader.readLine();
            while (line != null) {
                stringBuilder.append(line);
                stringBuilder.append(System.lineSeparator());
                line = bufferedReader.readLine();
            }
            return stringBuilder.toString();
        } catch (FileNotFoundException ex) {
            System.out.println("file not found");
        } catch (IOException ex) {
            System.out.println("There was an error and I need to drill down the right exception");
        }
        return null;
    }

    private void createSaveDirStructure() {
        try {
            Files.createDirectory(Paths.get("./saved_todos"));
            new File("./saved_todos/todo_titles.txt").createNewFile();
        } catch (IOException ex) {
            System.out.println("Error creating the directory and file. It may already exist.");
        }
    }

    public void printOpeningMenuVisual(List<String> todoTitles) {
        String dashesNeeded = dashesNeeded(todoTitles, false);
        System.out.println(dashesNeeded);
        formatTaskLines(todoTitles, LineTypes.TODO_TITLES).forEach(title -> {
            System.out.println(title);
            System.out.println(dashesNeeded);
        });
        System.out.println("");
        System.out.println("Which file would you like to open?");
        System.out.println("Enter anything not here for a new TODO");
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
            return "new todo"; // this String can be anything that isn't already a title of a TODO list
        }
    }

    // NOTE - if the file is not found, a new Todo session is launched
    public void openChosenFile(String fileName) {
        title = fileName; // displayed title match the name of the chosen file
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader("./saved_todos/" + fileName + ".txt"));
            String rawSavedTasks = bufferedReader.readLine(); // just need to read the first line
            List<String> rawTodosList = Arrays.asList(rawSavedTasks.split(Pattern.quote("|||")));
            // FORMAT =>    <name> ++ <status> ++ <linkUrl> |||
            rawTodosList.forEach( rawTodo -> {
                List<String> rawTodoElements = Arrays.asList(rawTodo.split(Pattern.quote("++")));
                addTask(rawTodoElements.get(0), Boolean.parseBoolean(rawTodoElements.get(1)), rawTodoElements.get(2), tasks.size());
            });
        } catch (FileNotFoundException ex) {
            System.out.println("Caught error in openChosenFile() " + ex);
        } catch (IOException ex) {
            System.out.println("There was an error in openChosenFile() and I need to drill down the right exception");
        }
    }

    /*
    < END    OPENING DIALOGUE  >
    < BEGIN  VALIDATIONS       >
     */

    private boolean isValidChosenTaskIndex(Integer requestedIndex) {
        if (requestedIndex == null) { return false; }
        return requestedIndex >= 0 && requestedIndex <= tasks.size();
    }

    /*
    <END   VALIDATIONS  >
     */

    // Just re-writes the entire file everytime. Doesn't append.
    public void writeToFile() {
        try {
            FileWriter writer = new FileWriter("./saved_todos/" + title + ".txt");
            BufferedWriter buffWriter = new BufferedWriter(writer);
            StringBuilder taskString = new StringBuilder();
            for (int i = (tasks.size() - 1); i >= 0; i--) {
                // format =   <name> ++ <status> ++ <linkUrl> |||
                String urlString = tasks.get(i).getLinkUrl() == null ? "null" : tasks.get(i).getLinkUrl().toString();
                taskString.insert(0, tasks.get(i).name + "++" + tasks.get(i).status.toString() + "++" + urlString + "|||");
            }
            buffWriter.write(taskString.toString());
            buffWriter.close();
        } catch (IOException e) {
            System.out.println("An error occurred writing the file.");
        }
    }

    public void changeTitle() {
        System.out.println("Enter the new title: ");
        String rawTitle = scanner.nextLine().toUpperCase();
        title = rawTitle.strip().replaceAll("\\s+", "_");

        // add the title name to the opening menu
        String titlesString = getGreetingMenuTitleString();
        titlesString = titlesString + title + ",";

        try {
            FileWriter writer = new FileWriter("./saved_todos/todo_titles.txt");
            BufferedWriter buffWriter = new BufferedWriter(writer);
            buffWriter.write(titlesString);
            buffWriter.close();
            writeToFile();
        } catch (IOException e) {
            System.out.println("An error occurred writing the todo_titles.txt file.");
        }
    }

    public void refreshVisual() {
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

        String dashesNeeded = dashesNeeded(tasks, true);
        System.out.println("*** " + title + " ***");
        System.out.println(dashesNeeded);
        formatTaskLines(null, LineTypes.TASKS).forEach(task -> {
            System.out.println(task);
            System.out.println(dashesNeeded);
        });
        System.out.println();
    }

    public void addTaskDialogue(String newItem) {
        if (newItem == null) {
            System.out.println("Enter the new ToDo item: ");
            newItem = scanner.nextLine();
            if (newItem.equals("cc")) return;
        }
        System.out.println("Position of the new item: ");
        int newPosition = 0;
        try {
            newPosition = Integer.parseInt(scanner.nextLine()) - 1;
        } catch (Exception e) {
            // do nothing. Leave it at index 0
        }
        if (newPosition < 0 || newPosition > tasks.size()) newPosition = tasks.size();
        addTask(newItem, false, null, newPosition);
    }

    public void addTask(String name, boolean completeStatus, String linkUrl, Integer newPosition) {
        if (newPosition == null) { newPosition = 0; }
        URI uri = null;
        try {
            uri = linkUrl == null || linkUrl.equals("null") ? null : new URI(linkUrl);
        } catch (URISyntaxException ex) {  } // do nothing
        tasks.add(newPosition, new Task(name, completeStatus, uri));
    }

    // TODO This method is ugly, but works. Make it cleaner.
    public List<String> formatTaskLines(List<String> listToFormat, LineTypes lineTypeNeeded) {
        // NOTE - passing in null listToFormat just uses the in-memory tasks Object
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
            String prePendTask = "";
            String postPendLink = "     ";
            if (lineTypeNeeded.equals(LineTypes.TASKS)) {
                String status = tasks.get(i).status ? "x" : " ";
                prePendTask = "| " + (i + 1) + tenSpace + " | " + status + " | ";
                postPendLink = tasks.get(i).getLinkUrl() == null ? "     " : "  (L)";
            } else if (lineTypeNeeded.equals(LineTypes.TODO_TITLES)) {
                prePendTask = "| " + (i + 1) + tenSpace + ": ";
            }
            formattedLines.add(prePendTask + listToFormat.get(i) + generateWhiteSpaceEnd(longest, listToFormat.get(i).length()) + postPendLink + "|");
        }
        return formattedLines;
    }

    public String generateWhiteSpaceEnd(int longest, int current) {
        return " ".repeat(longest - current + 2);
    }

    public void deleteItemDialogue(Integer removePosition) {
        String strRemovePosition = null;
        if (removePosition == null) {
            System.out.println("Task to remove: ");
            strRemovePosition = scanner.nextLine();
        }
        try {
            if (strRemovePosition != null) {
                removePosition = Integer.parseInt(strRemovePosition) -1;
            }
            if (!isValidChosenTaskIndex(removePosition)) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException ex) {
            System.out.println("Not a valid entry. Try again.");
            deleteItemDialogue(null);
            return;
        }
        deleteItem(removePosition);
    }

    public void deleteItem(int removeIndex) {
        recentlyDeleted.add(0, tasks.get(removeIndex));
        tasks.remove(removeIndex);
    }

    public void toggleComplete(Integer togglePosition) {
        String strTogglePosition = null;
        if (togglePosition == null) {
            System.out.println("Task to mark complete");
            strTogglePosition = scanner.nextLine();
        }
        try {
            if (strTogglePosition != null) {
                togglePosition = Integer.parseInt(strTogglePosition) -1;
            }
            if (!isValidChosenTaskIndex(togglePosition)) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException ex) {
            System.out.println("Not a valid entry. Try again.");
            toggleComplete(null);
            return;
        }
        tasks.get(togglePosition).setStatus(!tasks.get(togglePosition).getStatus());
    }

    public void moveTaskDialogue() {
        System.out.println("Which task do you want to move?");
        try {
            int fromPosition = Integer.parseInt(scanner.nextLine()) - 1;
            if (fromPosition < 0) {
                return;
            } else if (fromPosition >= tasks.size()) {
                throw new IllegalArgumentException();
            }
            System.out.println("New placement?");
            int newPosition = Integer.parseInt(scanner.nextLine()) - 1;
            if (newPosition < 0) {
                return;
            } else if (newPosition >= tasks.size()) {
                newPosition = tasks.size() - 1;
            }
            Task task = tasks.get(fromPosition);
            if (fromPosition == newPosition) {
                return;
            } else if (fromPosition < newPosition) {
                addTask(task.getName(), task.getStatus(), null, newPosition + 1);
                deleteItem(fromPosition);
            } else {
                deleteItem(fromPosition);
                addTask(task.getName(), task.getStatus(), null, newPosition);
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Not a valid entry. Try again.");
            moveTaskDialogue();
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

    private String dashesNeeded(List<?> referenceList, boolean containsTasks) {
        if (!referenceList.isEmpty() && referenceList.get(0) instanceof Task) {
            referenceList = referenceList.stream()
                    .map(task -> (Task)task)
                    .map(task -> task.getName())
                    .collect(Collectors.toList());
        }
        int dashesNeeded = referenceList.stream()
                .map(referenceItem -> (String) referenceItem)
                .map(String::length)
                .max(Integer::compare).orElse(1)
                + 12; // extra needed for (L) link

        if (containsTasks) { dashesNeeded += 6; }
        if (referenceList.size() >= 10) { dashesNeeded += 1; }
        return "-".repeat(dashesNeeded);
    }

    private void undoLastDeleted() {
        addTask(recentlyDeleted.get(0).getName(), false, null, 0);
    }

    private void openLinkDialogue() {
        // copied exactly from updateTaskName()
        System.out.println("Task link to open:");
        try {
            int openLinkPos = Integer.parseInt(scanner.nextLine()) - 1;
            if (openLinkPos < 0 || openLinkPos > tasks.size()) {
                throw new IllegalArgumentException();
            }
            URI link = tasks.get(openLinkPos).getLinkUrl();
            String[] command = { "open", link.toString() };
            new ProcessBuilder(command).start();
        } catch (IllegalArgumentException e) {
            System.out.println("Not a valid entry. Try again.");
            openLinkDialogue();
        } catch (IOException e) {
            System.out.println("Can't execute link open.");
            openLinkDialogue();
        }
    }

    private void addLinkDialogue() {
        // copied exactly from updateTaskName()
        System.out.println("Task to link:");
        try {
            int addLinkPos = Integer.parseInt(scanner.nextLine()) - 1;
            if (addLinkPos < 0 || addLinkPos > tasks.size()) {
                throw new IllegalArgumentException();
            }
            System.out.println("Link URL:");
            tasks.get(addLinkPos).setLinkUrl(new URI(scanner.nextLine().strip()));
        } catch (IllegalArgumentException e) {
            System.out.println("Not a valid entry. Try again.");
            addLinkDialogue();
        } catch (URISyntaxException e) {
            System.out.println("Not valid URI syntax. Try again.");
            addLinkDialogue();
        }
    }

    private void displayCommandMenu() {
        if (showFullMenu) {
            showFullMenu = false;
            System.out.println("c= complete     m= move task       u= update task");
            System.out.println("ol= open link   al= add link       ud= undo deleted");
            System.out.println("d= delete       sd= show deleted");
            System.out.println("rf = refresh visual    t= change title    ");
            System.out.println("x  = exit ToDo         cc or < 0 to cancel action");
            System.out.println("   ");
            System.out.println("Enter next task or command:  ");
        } else {
            System.out.println("(cmd) show commands  >>");
        }
    }

    private boolean shortCutExecuted(String nextAction) {
        if (nextAction.length() > 4 || nextAction.length() < 2) { // max 2 char shortcut command and 99 todos List length
            return false;
        }
        String firstLetter = nextAction.substring(0,1);
        String firstTwoLetters = nextAction.substring(0,2);
        Integer indexForOneLetterCommand = null;
        Integer indexForTwoLetterCommand = null;

        // NOTE ** reduce all commands by 1 because display is 1-indexed but List is 0-indexed
        try { indexForOneLetterCommand = Integer.parseInt(nextAction.substring(1)) - 1;
        } catch (NumberFormatException ignored) { } // do nothing. Leave as null

        try { indexForTwoLetterCommand = Integer.parseInt(nextAction.substring(2)) - 1;
        } catch (NumberFormatException | IndexOutOfBoundsException ignored) { } // do nothing. Leave as null

        if (firstLetter.equals("c") && indexForOneLetterCommand != null) {
            toggleComplete(indexForOneLetterCommand);
            return true;
        } else if (firstLetter.equals("d") && indexForOneLetterCommand != null) {
            deleteItemDialogue(indexForOneLetterCommand);
            return true;
        } else if (firstLetter.equals("u") && indexForOneLetterCommand != null) {
            // todo
            // update task
            // return true;
            return false;
        } else if (firstTwoLetters.equals("ol") && indexForTwoLetterCommand != null) {
            // todo
            // open link for task
            // return true;
            return false;
        }

        return false;
    }

    public void requestNextAction() {
        displayCommandMenu();
        String nextAction = scanner.nextLine().strip();
        nextAction = shortCutExecuted(nextAction) ? "shortCut" : nextAction;
        boolean showDeleted = false;
        switch (nextAction) {
            case "shortCut" -> {}
            case "a" -> addTaskDialogue(null);
            case "al" -> addLinkDialogue();
            case "c" -> toggleComplete(null);
            case "cmd" -> showFullMenu = true;
            case "d" -> deleteItemDialogue(null);
            case "m" -> moveTaskDialogue();
            case "t" -> changeTitle();
            case "ol" -> openLinkDialogue();
            case "rf" -> refreshVisual();
            case "sd" -> showDeleted = true;
            case "u" -> updateTaskName();
            case "ud" -> undoLastDeleted();
            case "x" -> quit = true;
            default -> addTaskDialogue(nextAction);
        }
        refreshVisual();
        writeToFile();
        if (showDeleted) { showRecentlyDeleted(); }
    }

    private static class Task {

        public Task(String name, Boolean completeStatus, URI linkUrl) {
            setName(name);
            setStatus(completeStatus);
            setLinkUrl(linkUrl);
        }

        private String name;
        private Boolean status;
        private URI linkUrl = null;

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

        public URI getLinkUrl() {
            return linkUrl;
        }

        public void setLinkUrl(URI linkUrl) {
            this.linkUrl = linkUrl;
        }

    }

    private enum LineTypes {
        TASKS,
        TODO_TITLES
    }

}
