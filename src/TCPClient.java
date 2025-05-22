import java.io.*;
import java.net.*;
import javax.net.ssl.*;
import java.util.*;

public class TCPClient {

    // ANSI para cores
    private static final String ANSI_RESET  = "\u001B[0m";
    private static final String ANSI_CYAN   = "\u001B[36m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_RED    = "\u001B[31m";
    private static final String ANSI_GREEN  = "\u001B[32m";

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
    
    //Função para buscar JSON via socket
    private static String fetchJson(String path) throws IOException {
             //Preparação para fazer conexão segura SSL
        SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        try (SSLSocket socket = (SSLSocket) factory.createSocket("pokeapi.co", 443);
             PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            //Envia requisição HTTP
            out.print("GET " + path + " HTTP/1.1\r\n");
            out.print("Host: pokeapi.co\r\n");
            out.print("Connection: close\r\n");
            out.print("\r\n");
            out.flush();

            //Lê status HTTP
            String statusLine = in.readLine();
            if (statusLine == null || !statusLine.startsWith("HTTP/1.1")) {
                throw new IOException("Resposta inválida do servidor");
            }
            int statusCode = Integer.parseInt(statusLine.split(" ")[1]);
            String line;
            while ((line = in.readLine()) != null && !line.isEmpty());
            if (statusCode == 404) return ""; //não encontrado
            if (statusCode != 200) throw new IOException("Erro HTTP: " + statusCode);

            //Lê corpo da resposta
            StringBuilder body = new StringBuilder();
            while ((line = in.readLine()) != null) body.append(line);
            return body.toString();
        }
    }

    //Função para pegar o nome do Pokemon no json
    public static String getNome(String json){
        String extractedName = extractField(json, "\"forms\":[{\"name\":\"", "\"");
        return extractedName;
    }

    //Função para pegar a id do Pokemon no json
    public static String getID(String json){
        String extractedId = extractField(json, "\"id\":", ",");
        return extractedId;
    }

    //Função para pegar o tipo do Pokemon no json
    public static String getTypes(String json){
        StringBuilder result = new StringBuilder();
        for (String t: extractAll(json, "\"type\":{\"name\":\"", "\"")){
            result.append(t).append(" ");
        }
        return result.toString().trim();
    }

    //Função para pegar as habilidades do Pokemon no json
    public static String getAbilities(String json){
        StringBuilder result = new StringBuilder();
        for (String a: extractAll(json, "\"ability\":{\"name\":\"", "\"")){
            result.append(a).append(" ");
        }
        return result.toString().trim();
    }

    //Função para pegar habilidades plus do Pokemon no json
    public static List<String> getAbilitiesList(String json){
        return extractAll(json, "\"ability\":{\"name\":\"", "\"");
    }

    //Função para retornar uma habilidade aleatória
    public static String getRandomAbility(String json){
        List<String> list = getAbilitiesList(json);
        if(list.isEmpty()) return "N/A";
        return list.get(new Random().nextInt(list.size()));
    }

    //Função para pegar habilidades plus do Pokemon no json
    public static String getMoves(String json){
        StringBuilder result = new StringBuilder();
        for (String m : extractAll(json, "\"move\":{\"name\":\"", "\"")) {
            result.append(m).append(" ");
        }
        return result.toString().trim();
    }

    //Função para pegar os status do Pokemon no json
    public static void getStatus(String json){
        List<String> statNames  = extractAll(json, "\"stat\":{\"name\":\"", "\"" );
        List<String> statValues = extractAll(json, "\"base_stat\":", ",");
        System.out.println(ANSI_GREEN + "Status:" + ANSI_RESET);
        for (int i = 0; i < statNames.size() && i < statValues.size(); i++) {
            System.out.printf(ANSI_YELLOW + "  %s" + ANSI_RESET + " = %s%n", statNames.get(i), statValues.get(i));
        }
    }

    //Função para exibir o link que mostra a foto do pokemon
    public static String getPicture(String json){
       String linkPicture =  extractField(json, "\"front_default\":\"", "\"");
       return linkPicture;
    }

    //Função para calcular fraquezas
    public static String getWeaknesses(String json) throws IOException{
        Set<String> weak = new HashSet<>();
        String typesBlock = extractField(json, "\"types\":", "]") + "]";
        for(String t: extractAll(typesBlock, "\"name\":\"", "\"")){
            String tj = fetchJson("/api/v2/type/"+t);
            String dd = extractField(tj, "\"double_damage_from\":[", "]");
            weak.addAll(extractAll(dd, "\"name\":\"", "\""));
        }
        return String.join(" ", weak);
    }

    //Função principal
    public static void main(String[] args) {
        BufferedReader inFromClient = new BufferedReader(new InputStreamReader(System.in));
        SSLSocketFactory connection = (SSLSocketFactory) SSLSocketFactory.getDefault();

        try {
            boolean running = true;
            while (running) {
                // Prompt para o usuário
                System.out.print(ANSI_CYAN + "Digite o nome do Pokémon ou 'sair': " + ANSI_RESET);
                String name = inFromClient.readLine().trim().toLowerCase();

                // Comando sair encerra o loop
                if (name.equals("sair")) {
                    running = false;
                    System.out.println(ANSI_GREEN + "Você se desconectou! Até logo :)" + ANSI_RESET);
                    continue;
                }

                // Status de conexão
                System.out.println(ANSI_YELLOW + "Conectando no servidor..." + ANSI_RESET);
                try (SSLSocket socket = (SSLSocket) connection.createSocket("pokeapi.co", 443)) {
                    PrintWriter outToServer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
                    BufferedReader inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    // Envia a requisição HTTP
                    outToServer.print("GET /api/v2/pokemon/" + name + " HTTP/1.1\r\n");
                    outToServer.print("Host: pokeapi.co\r\n");
                    outToServer.print("Connection: close\r\n");
                    outToServer.print("\r\n");
                    outToServer.flush();

                    // Lê resposta e separa cabeçalho de corpo
                    String responseLine; boolean isHeader = true;
                    StringBuilder jsonBody = new StringBuilder();
                    while ((responseLine = inFromServer.readLine()) != null) {
                        if (isHeader) {
                            if (responseLine.isEmpty()) isHeader = false;
                        } else {
                            jsonBody.append(responseLine);
                        }
                    }
                    String json = jsonBody.toString();

                    // Extrai nome para validação
                    String pokeName = getNome(json);
                    if (pokeName.equals("N/A")) {
                        System.out.println(ANSI_RED + "Ops... nome inválido. Digite de novo." + ANSI_RESET);
                    } else {
                        // Título dos dados
                        System.out.println(ANSI_GREEN + "--------- DADOS ENCONTRADOS ---------" + ANSI_RESET);

                        // Exibe Nome
                        System.out.println(ANSI_CYAN + "Nome do Pokémon:" + ANSI_RESET + " " + pokeName);

                        // Exibe Foto (URL)
                        System.out.println(ANSI_CYAN + "Foto:" + ANSI_RESET + " " + getPicture(json));

                        // Exibe ID
                        System.out.println(ANSI_CYAN + "ID:" + ANSI_RESET + " " + getID(json));

                        // Exibe Tipos com cor diferenciada
                        System.out.println(ANSI_YELLOW + "Tipo(s):" + ANSI_RESET + " " + getTypes(json));

                        // Exibe Ability aleatória com cor diferenciada
                        System.out.println(ANSI_YELLOW + "Ability:" + ANSI_RESET + " " + getRandomAbility(json));

                        // Exibe Moveset limitado em cor distinta
                        List<String> moves = extractAll(json, "\"move\":{\"name\":\"", "\"");
                        int limit = 10;
                        System.out.println(ANSI_YELLOW + "Moveset (primeiros " + limit + "):" + ANSI_RESET);
                        for (int i = 0; i < Math.min(limit, moves.size()); i++) {
                            System.out.println("  - " + moves.get(i));
                        }
                        if (moves.size() > limit) {
                            System.out.println(ANSI_CYAN + "Digite 'mostrar mais' para ver todos os moves." + ANSI_RESET);
                            if (inFromClient.readLine().trim().equalsIgnoreCase("mostrar mais")) {
                                System.out.println(ANSI_YELLOW + "Moveset completo:" + ANSI_RESET);
                                for (String m : moves) System.out.println("  - " + m);
                            }
                        }

                        // Exibe Fraquezas em cor vermelha destacada
                        System.out.println(ANSI_RED + "Fraquezas:" + ANSI_RESET + " " + getWeaknesses(json));

                        // Exibe status final
                        getStatus(json);
                        System.out.println();
                    }

                } catch (UnknownHostException ex) {
                    System.out.println(ANSI_RED + "Servidor não encontrado: " + ex.getMessage() + ANSI_RESET);
                } catch (IOException ioe) {
                    System.out.println(ANSI_RED + "Erro I/O: " + ioe.getMessage() + ANSI_RESET);
                }
            }
        } catch (IOException e) {
            System.out.println(ANSI_RED + "Erro de input: " + e.getMessage() + ANSI_RESET);
        }
    }
}

