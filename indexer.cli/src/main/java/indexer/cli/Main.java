package indexer.cli;

import indexer.core.api.FileIndexer;
import indexer.core.api.Tokenizer;
import indexer.core.FileIndexerImpl;
import indexer.core.TokenizerImpl;

import java.nio.file.Path;
import java.util.Scanner;
import java.util.Set;

public class Main {
    public static void main(String[] args) {
        //создаем токенизатор
        Tokenizer tokenizer = new TokenizerImpl();

        //когда программа завершится, Java сама вызовет метод close() у indexerа,
        //он аккуратно остановит все свои фоновые потоки из WatchService.
        try (FileIndexer indexer = new FileIndexerImpl(tokenizer);
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("Доступные команды:");
            System.out.println("  add <путь_к_папке>  - добавить папку для отслеживания");
            System.out.println("  search <слово>      - найти файлы с указанным словом");
            System.out.println("  exit                - выйти из программы");

            boolean running = true;
            while (running) {
                System.out.print("\n> ");
                String input = scanner.nextLine().trim();

                if (input.isEmpty()) {
                    continue; //если просто нажали Enter, ничего не делаем
                }

                //разбиваем ввод на команду и аргумент (путь или слово)
                String[] parts = input.split("\\s+", 2);
                String command = parts[0].toLowerCase();

                if (command.equals("exit")) {
                    System.out.println("Завершение работы");
                    running = false;
                } else if (command.equals("add")) {
                    if (parts.length < 2) {
                        System.out.println("Ошибка: укажите путь к папке");
                    } else {
                        Path dir = Path.of(parts[1]);
                        try {
                            indexer.watchDirectory(dir);
                            System.out.println("Папка успешно добавлена в индекс: " + dir.toAbsolutePath());
                            System.out.println("(Файлы индексируются в фоновом режиме)");
                        } catch (Exception e) {
                            System.out.println("Ошибка при добавлении папки: " + e.getMessage());
                        }
                    }
                } else if (command.equals("search")) {
                    if (parts.length < 2) {
                        System.out.println("Ошибка: укажите слово для поиска");
                    } else {
                        String word = parts[1];
                        Set<Path> results = indexer.search(word);

                        if (results.isEmpty()) {
                            System.out.println("Слово '" + word + "' не найдено ни в одном файле.");
                        } else {
                            System.out.println("Найдено в файлах (" + results.size() + "):");
                            for (Path p : results) {
                                System.out.println(" - " + p.toAbsolutePath());
                            }
                        }
                    }
                } else {
                    System.out.println("Неизвестная команда. Доступны: add, search, exit");
                }
            }
        } catch (Exception e) {
            System.out.println("Критическая ошибка при запуске индексатора: " + e.getMessage());
            e.printStackTrace();
        }
    }
}