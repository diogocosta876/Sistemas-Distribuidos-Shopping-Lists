import java.util.Scanner;
import java.util.List;

public class Client {
    private ShoppingListManager listManager;
    private ShoppingList selectedList;
    private Scanner scanner;

    public Client() {
        this.listManager = new ShoppingListManager();
        this.scanner = new Scanner(System.in);
    }

    public void run() {


        listManager.addShoppingList(listManager.loadShoppingListFromFile("./ShoppingLists/lists/lista.txt"));
        while (true) {


            if(selectedList != null){
                System.out.println("Selected List: "+this.selectedList.getName());
                this.selectedList.displayList();
            }


            System.out.println("1. Create a new shopping list");
            System.out.println("2. Select a shopping list");
            System.out.println("3. Add item to the selected list");
            System.out.println("4. Delete item from the selected list");
            System.out.println("5. Delete current selected list");
            System.out.println("6. Exit");
            System.out.print("Enter your choice: ");

            int choice = scanner.nextInt();
            scanner.nextLine(); // Consume newline

            if(selectedList == null && choice != 2 && choice != 1){
                System.out.println("No list selected. Please select a list first.");
            }else{
                switch (choice) {
                    case 1:
                        System.out.print("Enter the name of the shopping list: ");
                        String listName = scanner.nextLine();
                        listManager.createShoppingList(listName);

                        selectedList = listManager.getShoppingLists().get(listManager.getShoppingLists().size()-1);
                        listManager.saveShoppingListToFile(selectedList,"./ShoppingLists/lists/lista.txt");
                        break;
                    case 2:
                        selectShoppingList();
                        break;
                    case 3:
                        addItemToSelectedList();
                        listManager.saveShoppingListToFile(selectedList,"./ShoppingLists/lists/lista.txt");
                        break;
                    case 4:
                        deleteItemFromSelectedList();
                        listManager.saveShoppingListToFile(selectedList,"./ShoppingLists/lists/lista.txt");
                        break;
                    case 5:
                        if(selectedList != null){
                            listManager.deleteShoppingList(selectedList.getName());
                        }
                        break;
                    case 6:
                        System.out.println("Goodbye!");
                        return;
                    default:
                        System.out.println("Invalid choice. Please try again.");
                }
            }

        }
    }

    private void selectShoppingList() {

        this.listManager.displayShoppingLists();
        System.out.print("Enter the number of the list to select (0 to cancel): ");
        int choice = scanner.nextInt();
        scanner.nextLine(); // Consume newline

        if (choice > 0 && choice <= listManager.getShoppingLists().size()) {
            selectedList = listManager.getShoppingLists().get(choice - 1);
            System.out.println("Selected list: " + selectedList.getName());
        } else if (choice == 0) {
            System.out.println("Canceled selection.");
        } else {
            System.out.println("Invalid choice.");
        }
    }

    private void addItemToSelectedList() {
        if (selectedList == null) {
            return;
        }

        System.out.print("Enter the item name: ");
        String itemName = scanner.nextLine();
        System.out.print("Enter the item quantity: ");
        int itemQuantity = scanner.nextInt();
        scanner.nextLine(); // Consume newline

        Item item = new Item(itemName, itemQuantity);
        selectedList.addItem(item);
        System.out.println("Item added to the selected list.");
    }

    private void deleteItemFromSelectedList() {
        if (selectedList == null) {

            return;
        }


        System.out.print("Enter the number of the item to delete (0 to cancel): ");
        int choice = scanner.nextInt();
        scanner.nextLine(); // Consume newline

        if (choice > 0 && choice <= selectedList.getItems().size()) {
            selectedList.removeItem(choice - 1);
            System.out.println("Item deleted from the selected list.");
        } else if (choice == 0) {
            System.out.println("Canceled deletion.");
        } else {
            System.out.println("Invalid choice.");
        }
    }




}
