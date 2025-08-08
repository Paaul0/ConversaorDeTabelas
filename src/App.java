import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class App {
    public static void main(String[] args) throws IOException, InterruptedException {

        // --- Configuração ---
        String caminhoDoArquivo = "diario5.html";
        String htmlContent = Files.readString(Paths.get(caminhoDoArquivo), StandardCharsets.ISO_8859_1);
        String outputDir = "tabelas_png_firefox";
        Files.createDirectories(Paths.get(outputDir));

        String estiloPadrao = """
            <style>
                html, body {
                    overflow: hidden !important;
                }
                body { 
                    font-family: Arial, sans-serif; 
                    background-color: #f4f4f4; 
                    display: inline-block; 
                    padding: 0px; /* Espaçamento zerado ao redor da tabela */
                }
                table { 
                    border-collapse: collapse; 
                    margin: 0; /* Margin zerada para um corte perfeito */
                    font-size: 1em; 
                    min-width: 400px; 
                    box-shadow: 0 0 20px rgba(0, 0, 0, 0.15); 
                    background-color: #ffffff; 
                }
                /* Regra unificada para todas as células, garantindo alinhamento e cor */
                th, td { 
                    padding: 12px 15px; 
                    border: 1px solid #dddddd; 
                    text-align: left; 
                }
                
                /* REGRA DO THEAD VERDE FOI REMOVIDA DAQUI */
                
                /* Linhas pares (even) tanto do thead quanto do tbody ficarão cinzas */
                thead tr:nth-of-type(even),
                tbody tr:nth-of-type(even) { 
                    background-color: #f3f3f3; 
                }

                /* Borda aplicada apenas na ÚLTIMA linha da tabela inteira */
               tbody tr:last-of-type { 
                    border-bottom: 2px solid #009879; 
                }
            </style>
        """;
        // --- Fim da Configuração ---

        Document doc = Jsoup.parse(htmlContent);
        Elements tables = doc.select("table");
        System.out.println("Encontradas " + tables.size() + " tabelas. Iniciando conversão com Firefox...");

        System.setProperty("webdriver.gecko.driver", "geckodriver.exe");

        FirefoxOptions options = new FirefoxOptions();
        options.addArguments("--headless");

        WebDriver driver = new FirefoxDriver(options);

        for (int i = 0; i < tables.size(); i++) {
            Element table = tables.get(i);
            String tempHtmlContent = String.format(
                    "<html><head><meta charset='UTF-8'>%s</head><body>%s</body></html>",
                    estiloPadrao, table.outerHtml());

            Path tempFile = Files.createTempFile("table_" + i, ".html");
            Files.writeString(tempFile, tempHtmlContent);

            driver.get(tempFile.toUri().toString());

            Thread.sleep(300);

            // *** MUDANÇA PRINCIPAL AQUI: MUDAMOS O ALVO DA FOTO ***
            // Em vez de "html", agora tiramos a foto do "body", que está perfeitamente ajustado
            WebElement bodyElement = driver.findElement(By.tagName("body"));
            File screenshot = bodyElement.getScreenshotAs(OutputType.FILE);

            Path destinationFile = Paths.get(outputDir, "tabela_" + (i + 1) + ".png");
            Files.move(screenshot.toPath(), destinationFile, StandardCopyOption.REPLACE_EXISTING);

            System.out.println("Tabela " + (i + 1) + " salva em: " + destinationFile.toAbsolutePath());
            Files.delete(tempFile);
        }

        driver.quit();
        System.out.println("Processo concluído.");
    }
}