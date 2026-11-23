package com.raizes.raizesback;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class FluxoPedidoTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    private String token(String email, String senha) throws Exception {
        String body = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return om.readTree(body).get("accessToken").asText();
    }

    private static final String PEDIDO = "{\"canalPedido\":\"TOTEM\",\"unidadeId\":1,\"formaPagamento\":\"MOCK\","
            + "\"itens\":[{\"produtoId\":1,\"quantidade\":1}]}";

    @Test
    void semTokenRetorna401() throws Exception {
        mvc.perform(get("/pedidos")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("NAO_AUTENTICADO"));
    }

    @Test
    void clienteNaoCriaProduto403() throws Exception {
        String t = token("cliente@raizes.com", "Cliente@123");
        mvc.perform(post("/produtos").header("Authorization", "Bearer " + t).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"X\",\"preco\":10}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("ACESSO_NEGADO"));
    }

    @Test
    void canalPedidoAusenteRetorna422() throws Exception {
        String t = token("cliente@raizes.com", "Cliente@123");
        mvc.perform(post("/pedidos").header("Authorization", "Bearer " + t).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"unidadeId\":1,\"formaPagamento\":\"MOCK\",\"itens\":[{\"produtoId\":1,\"quantidade\":1}]}"))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.error").value("VALIDACAO_ERRO"));
    }

    @Test
    void estoqueInsuficienteRetorna409() throws Exception {
        String t = token("cliente@raizes.com", "Cliente@123");
        mvc.perform(post("/pedidos").header("Authorization", "Bearer " + t).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"canalPedido\":\"APP\",\"unidadeId\":1,\"formaPagamento\":\"MOCK\","
                                + "\"itens\":[{\"produtoId\":7,\"quantidade\":99}]}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ESTOQUE_INSUFICIENTE"));
    }

    @Test
    void fluxoCompletoPedidoPagamentoStatus() throws Exception {
        String cliente = token("cliente@raizes.com", "Cliente@123");
        String cozinha = token("cozinha@raizes.com", "Cozinha@123");

        String criado = mvc.perform(post("/pedidos").header("Authorization", "Bearer " + cliente)
                        .contentType(MediaType.APPLICATION_JSON).content(PEDIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AGUARDANDO_PAGAMENTO"))
                .andExpect(jsonPath("$.canalPedido").value("TOTEM"))
                .andReturn().getResponse().getContentAsString();
        JsonNode n = om.readTree(criado);
        long id = n.get("pedidoId").asLong();

        mvc.perform(post("/pagamentos").header("Authorization", "Bearer " + cliente)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"pedidoId\":" + id + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("APROVADO"))
                .andExpect(jsonPath("$.pedidoStatus").value("PAGO"));

        mvc.perform(patch("/pedidos/" + id + "/status").header("Authorization", "Bearer " + cozinha)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"EM_PREPARO\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("EM_PREPARO"));
    }

    @Test
    void pagamentoRecusadoCancelaPedido() throws Exception {
        String cliente = token("cliente@raizes.com", "Cliente@123");
        String criado = mvc.perform(post("/pedidos").header("Authorization", "Bearer " + cliente)
                        .contentType(MediaType.APPLICATION_JSON).content(PEDIDO))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = om.readTree(criado).get("pedidoId").asLong();

        mvc.perform(post("/pagamentos").header("Authorization", "Bearer " + cliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pedidoId\":" + id + ",\"simularResultado\":\"RECUSADO\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RECUSADO"))
                .andExpect(jsonPath("$.pedidoStatus").value("CANCELADO"));
    }
}
