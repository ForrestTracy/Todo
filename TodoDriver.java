public class TodoDriver {

    public static void main(String[] args) {
        Todo todo = new Todo();
        System.out.println("    ");
        todo.greetingMenu();
        todo.printVisual();
        while (!todo.quit) {
            todo.requestNextAction(true);
        }
    }

}
