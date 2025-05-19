import java.io.*;
import java.net.*;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class TCPClient {

    public static void main(String argv[ ]) throws Exception
    {

        //socket com protocolo TLS:
        SSLSocketFactory connection  = (SSLSocketFactory) SSLSocketFactory.getDefault();

        BufferedReader inFromClient = new BufferedReader(new InputStreamReader(System.in));

        //enviando o nome do pokemon:
        try {
            boolean condition = true;
            while (condition) {
                System.out.print("Type pokemon's name or 'sair' to exit: ");
                String name = inFromClient.readLine().toLowerCase();

                if (!name.equals("sair")) {
                    //Cria conexão na porta 443: é conexão segura https
                    SSLSocket clientSocket = (SSLSocket) connection.createSocket("pokeapi.co", 443);
                    PrintWriter outToServer = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
                    BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                    outToServer.print("GET /api/v2/pokemon/" + name + " HTTP/1.1\r\n");
                    outToServer.print("Host: pokeapi.co\r\n");
                    outToServer.print("Connection: close\r\n"); //fecha após responder a request
                    outToServer.print("\r\n");
                    outToServer.flush();

                    //recebendo a resposta:
                    try {
                        StringBuilder jsonBody = new StringBuilder();

                        String responseInJSON;

                        boolean isHeader = true;
                        while ((responseInJSON = inFromServer.readLine()) != null) {
                            //Queremos ler o body. Se o cabecalho acabou, leia body:
                            if (responseInJSON.isEmpty()) {
                                isHeader = false;
                                continue;
                            }
                            if (!isHeader) {
                                /*Alterar para as partes que a gente quer, por exemplo: nome e habilidades.
                                No site tem exemplo de conexao. Usei essa: /api/v2/pokemon/NOMEPOKEMON" */
                                System.out.println("Body"); //teste

                            }
                        }
                        System.out.println("Saiu do while");//teste

                    } catch (IOException e) {
                        System.out.println("Error");
                    } finally {
                        clientSocket.close();
                    }

                } else {
                    condition = false;
                    System.out.println("You disconnected.");
                }
            }
        }catch (IOException e) {
            System.out.println("Connection error or input error.");
        }

    }
}
