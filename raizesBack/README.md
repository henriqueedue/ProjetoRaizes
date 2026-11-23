# Raízes do Nordeste — Back-end (API REST)

API Spring Boot da rede de lanchonetes **Raízes do Nordeste**, multicanal (`APP`, `TOTEM`, `BALCAO`, `PICKUP`, `WEB`).
**Fluxo crítico entregue (Fluxo A):** Pedido → Pagamento mock → Atualização de status (com estoque por unidade, fidelidade e auditoria).

## Requisitos
- Java 21 (JDK) — Maven não precisa estar instalado (usa `mvnw`)
- Banco: **H2 em memória (padrão, zero configuração)** ou PostgreSQL 16 (opcional, via `docker compose`)

## Como executar
```powershell
cd raizesBack
.\mvnw.cmd spring-boot:run        # Windows  (Linux/Mac: ./mvnw spring-boot:run)
```
- API: http://localhost:8080
- **Swagger/OpenAPI:** http://localhost:8080/swagger-ui.html  (JSON: `/v3/api-docs`)
- As **migrations (Flyway)** rodam automaticamente na subida e o **seed** é aplicado com o banco vazio.

### Variáveis de ambiente
Veja `.env.example` (todas opcionais). Para usar PostgreSQL:
```powershell
docker compose up -d
$env:DB_URL="jdbc:postgresql://localhost:5432/raizes"; $env:DB_USER="raizes"; $env:DB_PASSWORD="raizes123"
.\mvnw.cmd spring-boot:run
```

### Testes automatizados
```powershell
.\mvnw.cmd test
```

## Usuários do seed
| Perfil | E-mail | Senha |
|---|---|---|
| ADMIN | admin@raizes.com | Admin@123 |
| GERENTE | gerente@raizes.com | Gerente@123 |
| ATENDENTE | atendente@raizes.com | Atendente@123 |
| COZINHA | cozinha@raizes.com | Cozinha@123 |
| CLIENTE | cliente@raizes.com | Cliente@123 |

Dados: unidades 1 (Recife) e 2 (Fortaleza); produtos 1–7; o produto **7** tem apenas 2 unidades no estoque da unidade 1 (usado no teste de conflito 409).

## Arquitetura (camadas)
```
com.raizes.raizesback
├── domain/          entidades JPA, enums e regras (StatusPedido: máquina de estados)
├── application/     casos de uso: PedidoService, PagamentoService, EstoqueService, FidelidadeService, AuditoriaService...
├── infrastructure/  repositories (Spring Data), segurança JWT, gateway de pagamento mock, seed
└── api/             controllers, DTOs (contratos), tratamento global de erros, OpenAPI
```

## Padrões da API
- Rotas no plural, ids no path, paginação `?page=1&limit=10` (limit máx. 100), resposta `{content,page,limit,totalElements,totalPages}`.
- **Erro padrão** (todas as falhas, inclusive 401/403):
```json
{ "error": "ESTOQUE_INSUFICIENTE", "message": "...", "details": [{"field":"itens[0].quantidade","issue":"Disponível: 1"}],
  "timestamp": "2026-02-05T12:00:00Z", "path": "/pedidos" }
```
- Status: 200, 201, 202 (pagamento pendente), 204, 400 (JSON/enum inválido), 401, 403, 404, 409 (regra de negócio), 422 (validação).

## Endpoints principais
| Recurso | Método e rota | Perfis |
|---|---|---|
| Auth | `POST /auth/register`, `POST /auth/login` | público |
| Usuários | `GET /usuarios/me`, `POST /usuarios` (equipe), `DELETE /usuarios/me` (anonimiza – LGPD) | autenticado / ADMIN |
| Unidades | `GET /unidades`, `GET /unidades/{id}/cardapio`, `POST /unidades` | público / GERENTE, ADMIN |
| Produtos | `GET/POST /produtos`, `GET/PUT/DELETE /produtos/{id}` | autenticado / GERENTE, ADMIN |
| Estoque | `GET /estoque?unidadeId=`, `POST /estoque/movimentacoes` | ATENDENTE+ / GERENTE, ADMIN |
| Pedidos | `POST /pedidos`, `GET /pedidos?canalPedido=&status=&unidadeId=`, `GET /pedidos/{id}`, `PATCH /pedidos/{id}/status`, `PATCH /pedidos/{id}/cancelar` | CLIENTE, ATENDENTE, GERENTE, ADMIN (status: + COZINHA) |
| Pagamentos | `POST /pagamentos`, `GET /pagamentos/pedido/{id}` | CLIENTE, ATENDENTE, GERENTE, ADMIN |
| Fidelidade | `GET /fidelidade/saldo`, `PUT /fidelidade/consentimento`, `POST /fidelidade/resgatar`, `GET /fidelidade/historico` | CLIENTE |
| Auditoria | `GET /auditoria` | ADMIN |

Exemplo de criação de pedido (`canalPedido` obrigatório):
```json
{ "canalPedido": "TOTEM", "unidadeId": 1, "formaPagamento": "MOCK", "itens": [ { "produtoId": 1, "quantidade": 2 } ] }
```

## Regras de negócio
- **Estoque por unidade:** pedido reserva (saída) o estoque com lock pessimista; sem saldo → `409 ESTOQUE_INSUFICIENTE`. Cancelamento ou pagamento recusado devolvem o estoque.
- **Status:** `AGUARDANDO_PAGAMENTO → PAGO (via pagamento) → EM_PREPARO → PRONTO → ENTREGUE`; `CANCELADO` a partir de AGUARDANDO_PAGAMENTO/PAGO/EM_PREPARO (cliente não cancela em EM_PREPARO).
- **Pagamento mock** (`simularResultado`): `APROVADO` (padrão) → pedido PAGO; `RECUSADO` → pedido CANCELADO + estoque devolvido; `FALHA_GATEWAY` → pagamento PENDENTE (202), pedido mantido para nova tentativa (tolerância a falhas). Pedido já pago não aceita novo pagamento (409, evita cobrança duplicada).
- **Fidelidade:** com consentimento, 1 ponto por R$ 1,00 ao `ENTREGUE`; resgate simples: 1 ponto = R$ 0,05.
- **Promoções/campanhas (regra documentada):** campanhas são aplicadas sobre o total do pedido antes do pagamento — ex.: *"Terça Raiz: 10% off em Tapioca na unidade X"*; implementação prevista como entidade `Campanha` (unidade, produto, percentual, vigência) consultada em `PedidoService.criar`. Não implementada no MVP (priorizado o fluxo crítico).

## Segurança e LGPD
- Senha com **BCrypt**; autenticação **JWT** stateless; autorização por perfil (`@PreAuthorize`) e verificação de posse (cliente só vê/paga/cancela os próprios pedidos).
- **Dados coletados:** nome, e-mail e senha (hash) — finalidade: identificação e execução do pedido (base legal: execução de contrato); pontos de fidelidade — base legal: **consentimento** (registrado em `consentimento_lgpd_em` e revogável em `PUT /fidelidade/consentimento`).
- **Minimização/exposição:** respostas nunca retornam hash de senha; token carrega apenas id, e-mail e perfil.
- **Direito do titular:** `DELETE /usuarios/me` anonimiza os dados pessoais (pedidos permanecem sem identificação, para retenção fiscal).
- **Auditoria:** tabela `logs_auditoria` (login, criação/cancelamento de pedido, mudança de status, pagamento, estoque, fidelidade), consultável em `GET /auditoria` (ADMIN).

## Evidências e testes
- **Coleção Postman:** `postman/RaizesDoNordeste.postman_collection.json` (23 cenários T01–T23, com asserts). Importe no Postman e rode a coleção inteira com o *Collection Runner*, na ordem das pastas (a variável `baseUrl` é `http://localhost:8080`; os tokens são salvos automaticamente após o login).
- Cobertura: login/token (200), sem token (401), sem permissão (403), validação (400/422), produto/unidade inexistente (404), estoque insuficiente (409), pagamento aprovado/recusado/falha de gateway, filtro por canal, auditoria.
- **Testes automatizados:** `src/test/java/.../FluxoPedidoTest.java`.
- Links (preencher na entrega): Repositório: `https://github.com/henriqueedue/ProjetoRaizes` · Swagger local: `http://localhost:8080/swagger-ui.html`
