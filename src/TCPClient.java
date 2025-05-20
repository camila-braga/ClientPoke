import java.io.*;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.util.List;
import java.util.ArrayList;
import java.io.OutputStreamWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;

public class TCPClient {

    public static void main(String[] args) {
        BufferedReader inFromClient = new BufferedReader(new InputStreamReader(System.in));
        SSLSocketFactory connection = (SSLSocketFactory) SSLSocketFactory.getDefault();
	// Isso é para enviar o nome do pokemon - Assim como você já tinha feito
        try {
            boolean running = true; // Alterei a condição de boolean condition para boolean ruuning.
            while (running) {
                System.out.print("Digite o nome do Pokémon ou 'sair' para pular fora: "); // Padronizei em português - Para apresentar
                String name = inFromClient.readLine().trim().toLowerCase();

                if (name.equals("sair")) {
                    running = false;
                    System.out.println("Você se desconectou! Até logo :)");
                    continue;
                } // Mudei um pouco o while que você fez para poder deixar o print dele aqui - "Condition = false" ... systemout...

                // Cria conexão na porta 443: é conexão segura https > Adicionei o try para tentar lidar com o Packet Loss
                try (SSLSocket socket = (SSLSocket) connection.createSocket("pokeapi.co", 443); 
                     PrintWriter outToServer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
                     BufferedReader inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                    // Get apenas padrão
                    outToServer.print("GET /api/v2/pokemon/" + name + " HTTP/1.1\r\n");
                    outToServer.print("Host: pokeapi.co\r\n");
                    outToServer.print("Connection: close\r\n");
                    outToServer.print("\r\n");
                    outToServer.flush();

                    // Recebendo a resposta
                    String responseLine;
                    boolean isHeader = true;
                    StringBuilder jsonBody = new StringBuilder();

		    /// Separando do Corpo
                    while ((responseLine = inFromServer.readLine()) != null) {
                        if (isHeader) {
                            if (responseLine.isEmpty()) {
                                isHeader = false;
                            }
                        } else {
                            jsonBody.append(responseLine);
                        }
                    }

                    String json = jsonBody.toString();
                
                    // Puxando com helper para extrair e imprimir os dados
		    // --- Nome // Esse nome ainda tá meio buxa > Tava testando com o Mudkip .-. < Ele tenta pular as repetições
		    int idxId = json.indexOf("\"id\":");
		    int idxName = json.indexOf("\"name\":\"", idxId);
                    String extractedName = json.substring(idxName + 8, json.indexOf("\"", idxName + 8));
                    System.out.println("Nome do Pokémon: " + extractedName);

		    // --- Num dex
		    String extractedId = extractField(json, "\"id\":", ",");
		    System.out.println("ID do Pokémon: " + extractedId);

		    // --- Tipos
		    System.out.print("Tipos: ");
		    for (String t: extractAll(json, "\"type\":{\"name\":\"", "\"")){
		    	System.out.print(t + " ");
		    }
		    System.out.println();
		    //
		    // --- Ability (Inata)
		    System.out.print("Habilidade: ");
		    for (String a: extractAll(json, "\"ability\":{\"name\":\"", "\"")){
			 System.out.print(a + " " + "-");
		    }
		    System.out.println();
		    //
		    // --- Habilidades
		    System.out.print("Moveset: ");
                    for (String m : extractAll(json, "\"move\":{\"name\":\"", "\"")) {
       			 System.out.print(m + " ");
		    }
		    System.out.println();

		    //
		    // --- Status base
		    List<String> statNames  = extractAll(json, "\"stat\":{\"name\":\"", "\"");
		    List<String> statValues = extractAll(json, "\"base_stat\":", ",");
		    System.out.println("Status:");
		    for (int i = 0; i < statNames.size() && i < statValues.size(); i++) {
    			System.out.printf("  %s = %s%n", statNames.get(i), statValues.get(i));
		    }
                    System.out.println("\n");

                } catch (IOException ioe) {
                    System.err.println("Erro na conexão ou leitura: " + ioe.getMessage());
                } 
            }
        } catch (IOException e) {
            System.err.println("Erro de input: " + e.getMessage());
        }
    }
// Esse helper - tem que melhor ambos - pega só a primeira vez que aparece
   private static String extractField(String json, String prefix, String suffix) {
   	 int start = json.indexOf(prefix);
    	if (start < 0) return "N/A";
    	start += prefix.length();
    	int end = json.indexOf(suffix, start);
    	if (end < 0) return "N/A";
    	return json.substring(start, end);
}
     /// Little função para tentar pegar as ocorrências que sempre ficam repetindo
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

}
