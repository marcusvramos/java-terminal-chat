# Chat Terminal Java (Cliente-Servidor com Threads)

## Requisitos

- **Java 8+**
- **SQLite JDBC Driver**
  - Baixe e coloque `sqlite-jdbc-3.49.1.0.jar` na raiz do projeto.
  - [Download aqui](https://github.com/xerial/sqlite-jdbc)

---

## Estrutura do Projeto

- /Projeto
    - core/
        - ChatServer.java
        - ChatClient.java
        - ClientHandler.java
    - db/
        - Database.java
    - entities/
        - User.java
        - Group.java
        - Message.java
    - service/
        - Utils.java
    - bin/
    - sqlite-jdbc-3.49.1.0.jar
    - chat.db
    - README.md

---

## Como Compilar

Abra o terminal na pasta do projeto e digite:

    javac -cp ".:sqlite-jdbc-3.49.1.0.jar" -d bin core/*.java db/*.java entities/*.java service/*.java

---

## Como Executar

Abra **dois terminais** (ou mais):

No primeiro, rode o servidor:

    java -cp "bin:sqlite-jdbc-3.49.1.0.jar" core.ChatServer

No(s) outro(s), rode o cliente:

    java -cp "bin:sqlite-jdbc-3.49.1.0.jar" core.ChatClient

---

## Comandos Disponíveis no Chat

- **/cadastro**  
  Cadastre-se informando nome, login, email, senha.

- **/login**  
  Faça login.

- **/recuperar**  
  Redefina sua senha informando nome completo e email.

- **/status online/ocupado**  
  Altere seu status.

- **/listarusuarios**  
  Veja quem está online.

- **/listargrupos**  
  Veja os grupos existentes.

- **/criargroup nome**  
  Crie um grupo.

- **/adicionar login grupo**  
  Convide alguém para um grupo.

- **/listarmembros grupo**  
  Veja membros do grupo (apenas se você for membro).

- **/sairgrupo grupo**  
  Saia do grupo.

- **/msg login1,login2 mensagem**  
  Envie mensagem privada (precisa aceite do(s) destinatário(s)).

- **/aceitar login**  
  Aceite um chat privado.

- **/msggroup grupo login@mensagem**  
  Envie mensagem só para certos membros do grupo.

- **/solicitarentrada grupo**  
  Solicite entrada em grupo (todos os membros devem aceitar).

- **/aceitarentrada grupo login**  
  Aceite entrada de um usuário no grupo.

- **/recusarentrada grupo login**  
  Recuse entrada de um usuário no grupo.

- **/sair**  
  Sair do chat.
