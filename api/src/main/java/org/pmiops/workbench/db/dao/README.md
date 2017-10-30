DAOs' query implementations are
[automatically derived](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#repositories.query-methods.details).

Services implement custom behavior which can't be auto-generated. Where both a
Service and a DAO are available, inject the Service and use its public dao field
where necessary. [Design doc](https://docs.google.com/document/d/1uNf6_5TZxnQt8BP_wWoGxPcGwaIZAhZswW3bXwbswHU).
