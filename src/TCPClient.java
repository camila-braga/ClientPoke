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
        //Prepara para ler o input do usuário pelo teclado
        BufferedReader inFromClient = new BufferedReader(new InputStreamReader(System.in));
        //Prepara para fazer conexão segura SSL
        SSLSocketFactory connection = (SSLSocketFactory) SSLSocketFactory.getDefault();

	// Isso é para enviar o nome do pokemon - Assim como você já tinha feito

        //Pensei e refazer esse try e catch do jeito que mostrei no discord. Pra ficar mais legível.
        try {
            boolean running = true; // Alterei a condição de boolean condition para boolean running.
            while (running) {
                System.out.print("Digite o nome do Pokémon ou 'sair' para pular fora: "); // Padronizei em português - Para apresentar
                String name = inFromClient.readLine().trim().toLowerCase(); //Lê o nome do pokemon, ignorando espaços vazios e transformando as letras em minusculas

                if (name.equals("sair")) { //Digitar "sair" encerra a pesquisa pelo pokemon
                    running = false;
                    System.out.println("Você se desconectou! Até logo :)");
                    continue;
                } // Mudei um pouco o while que você fez para poder deixar o print dele aqui - "Condition = false" ... systemout...

                // Cria conexão na porta 443: é conexão segura https > Adicionei o try para tentar lidar com o Packet Loss
                try (SSLSocket socket = (SSLSocket) connection.createSocket("pokeapi.co", 443); 
                     PrintWriter outToServer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
                     BufferedReader inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                    // Get apenas padrão
                    outToServer.print("GET /api/v2/pokemon/" + name + " HTTP/1.1\r\n"); //Envia uma requisição HTTP/1.1 para buscar o Pokémon com o nome digitado
                    outToServer.print("Host: pokeapi.co\r\n"); //\r\n marca o fim de cada linha, \r\n\r\n marca o fim do cabeçalho HTTP.

                    //print pra gente saber se conectou:
                    System.out.println("Conexão com servidor bem sucedida!");

                    outToServer.print("Connection: close\r\n");  //encerra a conexão

                    outToServer.print("\r\n");
                    outToServer.flush();

                    // Recebendo a resposta
                    String responseLine;
                    boolean isHeader = true;
                    StringBuilder jsonBody = new StringBuilder();

		    // Separando do Corpo
                    while ((responseLine = inFromServer.readLine()) != null) { //Lê a resposta linha por linha
                        if (isHeader) {
                            if (responseLine.isEmpty()) {
                                isHeader = false; //fim do cabeçalho quando encontra linha em branco
                            }
                        } else {
                            jsonBody.append(responseLine);
                        }
                    }

                    String json = jsonBody.toString(); //Transforma o StringBuilder em String para facilitar o tratamento.
                
                    // Puxando com helper para extrair e imprimir os dados
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
		    System.out.println();
		    //
		    // --- Ability (Inata)
		    System.out.print("Habilidade: ");
		    for (String a: extractAll(json, "\"ability\":{\"name\":\"", "\"")){ //Pega todas as habilidades
			 System.out.print(a + " " + "-");
		    }
		    System.out.println();
		    //
		    // --- Habilidades
		    System.out.print("Moveset: ");
                    for (String m : extractAll(json, "\"move\":{\"name\":\"", "\"")) { //Pega todos os movimentos
       			 System.out.print(m + " ");
		    }
		    System.out.println();

		    //
		    // --- Status base
		    List<String> statNames  = extractAll(json, "\"stat\":{\"name\":\"", "\""); //Extrai os nomes dos atributos e seus valores.
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
   private static String extractField(String json, String prefix, String suffix) { //extrai um valor único
   	 int start = json.indexOf(prefix);
    	if (start < 0) return "N/A";
    	start += prefix.length();
    	int end = json.indexOf(suffix, start);
    	if (end < 0) return "N/A";
    	return json.substring(start, end);
}
     /// Little função para tentar pegar as ocorrências que sempre ficam repetindo
     private static List<String> extractAll(String json, String prefix, String suffix) { //extrai vários valores
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
