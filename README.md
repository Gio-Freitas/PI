# Módulo de Landing Pages e Formulários

## Pré-requisitos
- Java 17
- Maven
- Docker

## Como rodar o projeto

1. **Subir o banco de dados local (docker)**
    Na raiz do projeto, execute o comando para subir o PostgreSQL:
    ```bash
    docker-compose up -d
    ```
2. **Executar a aplicação Spring Boot:**
    Após o banco estar rodando, compile e inicie a aplicação com o Maven:
    ```bash
    mvn clean spring-boot:run
    ```
    O Flyway irá rodar automaticamente na inicialização, criando o schema landing e as tabelas core.
3. **Acessar o banco**
    - **Host:** localhost
    - **Porta:** 5432
    - **Database:** landing_forms_db
    - **User:** omni
    - **Senha:** omni123