import java.io.*;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.util.List;
import java.util.ArrayList;
import java.io.OutputStreamWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;

public class TCPClient {

    //Função para pegar só a primeira ocorrência
    private static String extractField(String json, String prefix, String suffix) {
        int start = json.indexOf(prefix);
        if (start < 0) return "N/A";
        start += prefix.length();
        int end = json.indexOf(suffix, start);
        if (end < 0) return "N/A";
        return json.substring(start, end);
    }
    //Função para tentar pegar as ocorrências que sempre ficam repetindo
    private static List<String> extractAll(String json, String prefix, String suffix) {
        List<String> items = new ArrayList<>();
        int idx = 0;
        while ((idx = json.indexOf(prefix, idx)) != -1) {
            idx += prefix.length();
            int end = json.indexOf(suffix, idx);
            if (end == -1) break;
            items.add(json.substring(idx, end));
            idx = end + suffix.length();
        }
        return items;
    }
    //Função para pegar o nome do Pokemon no json
    public static String getNome(String json){
        int idxId = json.indexOf("\"id\":"); // evita pegar o nome errado de outro campo
        int idxName = json.indexOf("\"name\":\"", idxId);
        String extractedName = json.substring(idxName + 8, json.indexOf("\"", idxName + 8)); //extrai o valor entre aspas.
        return extractedName;
    }

    //Função para pegar a id do Pokemon no json
    public static String getID(String json){
        String extractedId = extractField(json, "\"id\":", ","); // extrai o número do ID entre "id": e a próxima vírgula.
        return extractedId;
    }

    //Função para pegar o tipo do Pokemon no json
    public static String getTypes(String json){
        StringBuilder result = new StringBuilder();
        for (String t: extractAll(json, "\"type\":{\"name\":\"", "\"")){ //extrai todos os tipos
            result.append(t).append(" ");
        }
        String types = result.toString();
        return types;
    }

    //Função para pegar as habilidades do Pokemon no json
    public static String getAbilities(String json){
        StringBuilder result = new StringBuilder();
        for (String a: extractAll(json, "\"ability\":{\"name\":\"", "\"")){ //Pega todas as habilidades
            result.append(a).append(" ");
        }
        String abilities = result.toString();
        return abilities;
    }

    //Função para pegar habilidades plus do Pokemon no json
    public static String getMoves(String json){
        StringBuilder result = new StringBuilder();
        for (String m : extractAll(json, "\"move\":{\"name\":\"", "\"")) { //Pega todos os movimentos
            result.append(m).append(" ");
        }
        String moves = result.toString();
        return moves;
    }

    //Função para pegar os status do Pokemon no json
    public static void getStatus(String json){
        List<String> statNames  = extractAll(json, "\"stat\":{\"name\":\"", "\""); //Extrai os nomes dos atributos e seus valores.
        List<String> statValues = extractAll(json, "\"base_stat\":", ",");
        for (int i = 0; i < statNames.size() && i < statValues.size(); i++) {
            System.out.printf("  %s = %s%n", statNames.get(i), statValues.get(i));
        }
    }

    //Função principal
    public static void main(String[] args) {
        //Preparação para ler o input do usuário pelo teclado
        BufferedReader inFromClient = new BufferedReader(new InputStreamReader(System.in));
        //Preparação para fazer conexão segura SSL
        SSLSocketFactory connection = (SSLSocketFactory) SSLSocketFactory.getDefault();

	    //Pensei e refazer esse try e catch do jeito que mostrei no discord. Pra ficar mais legível.
        try {
            boolean running = true;
            while (running) {
                System.out.print("Digite o nome do Pokémon ou 'sair' para pular fora: ");
                System.out.println();
                String name = inFromClient.readLine().trim().toLowerCase(); //Leitura do nome do pokemon

                //Digitar "sair" encerra a pesquisa pelo pokemon
                if (name.equals("sair")) {
                    running = false;
                    System.out.println("Você se desconectou! Até logo :)");
                    continue;
                }

                System.out.println("Conectando no servidor...");
                System.out.println();

                // Cria conexão na porta 443: é conexão segura https
                try (SSLSocket socket = (SSLSocket) connection.createSocket("pokeapi.co", 443);)
                {
                    System.out.println("Socket criado com sucesso");
                    System.out.println();

                    //Prepara os canais de entrada e saída de dados:
                    PrintWriter outToServer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
                    BufferedReader inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    // Get apenas padrão HTTP
                    //Envia uma requisição HTTP/1.1 para buscar o Pokémon com o nome digitado
                    outToServer.print("GET /api/v2/pokemon/" + name + " HTTP/1.1\r\n");
                    //\r\n marca o fim de cada linha, \r\n\r\n marca o fim do cabeçalho HTTP.
                    outToServer.print("Host: pokeapi.co\r\n");
                    System.out.println("Conexão com servidor bem sucedida!");
                    System.out.println();
                    outToServer.print("Connection: close\r\n");  //encerra a conexão
                    outToServer.print("\r\n");
                    outToServer.flush();

                    // Recebendo a resposta
                    String responseLine;
                    boolean isHeader = true;
                    StringBuilder jsonBody = new StringBuilder();

		            // Separando cabeçalho do corpo do json:
                    //Leitura da resposta linha por linha
                    while ((responseLine = inFromServer.readLine()) != null) {
                        if (isHeader) {
                            //fim do cabeçalho quando encontra linha em branco
                            if (responseLine.isEmpty()) {
                                isHeader = false;
                            }
                        } else {
                            jsonBody.append(responseLine);
                        }
                    }
                    //Transforma o StringBuilder em String para facilitar o tratamento.
                    String json = jsonBody.toString();

                    //Impressão dos resultados encontrados:
                    System.out.println("---------DADOS ENCONTRADOS---------");
                    System.out.println();

                    String pokeName = getNome(json);
                    System.out.println("Nome do Pokémon: " + pokeName);
                    System.out.println();

                    String pokeID = getID(json);
                    System.out.println("ID do Pokémon: " + pokeID);
                    System.out.println();

                    System.out.print("Tipos: ");
                    String pokeTypes = getTypes(json);
                    System.out.println(pokeTypes);
                    System.out.println();

                    System.out.print("Habilidade: ");
                    String pokeAbilities = getAbilities(json);
                    System.out.println(pokeAbilities);
                    System.out.println();

                    System.out.print("Moveset: ");
                    String pokeMoves = getMoves(json);
                    System.out.println(pokeMoves);
                    System.out.println();

                    System.out.println("Status:");
                    getStatus(json);
                    System.out.println();


                    /*
                    // --- Nome // Esse nome ainda tá meio buxa > Tava testando com o Mudkip .-. < Ele tenta pular as repetições
                    int idxId = json.indexOf("\"id\":"); // evita pegar o nome errado de outro campo
		            int idxName = json.indexOf("\"name\":\"", idxId);
                    String extractedName = json.substring(idxName + 8, json.indexOf("\"", idxName + 8)); //extrai o valor entre aspas.
                    System.out.println("Nome do Pokémon: " + extractedName);

		            // --- Num dex
		            String extractedId = extractField(json, "\"id\":", ","); // extrai o número do ID entre "id": e a próxima vírgula.
		            System.out.println("ID do Pokémon: " + extractedId);

                    // --- Tipos
                    System.out.print("Tipos: ");
                    for (String t: extractAll(json, "\"type\":{\"name\":\"", "\"")){ //extrai todos os tipos
                        System.out.print(t + " ");
                    }

                    // --- Ability (Inata)
                    System.out.print("Habilidade: ");
                    for (String a: extractAll(json, "\"ability\":{\"name\":\"", "\"")){ //Pega todas as habilidades
                        System.out.print(a + " " + "-");
                    }
                    System.out.println();

                    // --- Habilidades
                    System.out.print("Moveset: ");
                    for (String m : extractAll(json, "\"move\":{\"name\":\"", "\"")) { //Pega todos os movimentos
                         System.out.print(m + " ");
                    }
                    System.out.println();

                    // --- Status base
                    List<String> statNames  = extractAll(json, "\"stat\":{\"name\":\"", "\""); //Extrai os nomes dos atributos e seus valores.
                    List<String> statValues = extractAll(json, "\"base_stat\":", ",");
                    System.out.println("Status:");
                    for (int i = 0; i < statNames.size() && i < statValues.size(); i++) {
                        System.out.printf("  %s = %s%n", statNames.get(i), statValues.get(i));
                    }
                    System.out.println("\n");*/

                } catch (IOException ioe) {
                    System.err.println("Erro na conexão ou leitura: " + ioe.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Nome não existe. Erro de input: " + e.getMessage());
        }
    }
}
