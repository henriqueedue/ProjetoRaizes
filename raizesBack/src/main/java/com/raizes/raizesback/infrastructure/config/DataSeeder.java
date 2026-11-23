package com.raizes.raizesback.infrastructure.config;

import com.raizes.raizesback.domain.*;
import com.raizes.raizesback.infrastructure.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

/** Seed de demonstracao (executa apenas com o banco vazio). */
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {
    private final UsuarioRepository usuarios;
    private final UnidadeRepository unidades;
    private final ProdutoRepository produtos;
    private final EstoqueRepository estoques;
    private final PasswordEncoder encoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (usuarios.count() > 0) return;

        usuario("Administrador", "admin@raizes.com", "Admin@123", Perfil.ADMIN, false);
        usuario("Gerente Recife", "gerente@raizes.com", "Gerente@123", Perfil.GERENTE, false);
        usuario("Atendente Balcão", "atendente@raizes.com", "Atendente@123", Perfil.ATENDENTE, false);
        usuario("Cozinha Recife", "cozinha@raizes.com", "Cozinha@123", Perfil.COZINHA, false);
        usuario("Maria Cliente", "cliente@raizes.com", "Cliente@123", Perfil.CLIENTE, true);

        Unidade recife = unidade("Raízes - Recife Antigo", "Recife");
        Unidade fortaleza = unidade("Raízes - Fortaleza Centro", "Fortaleza");

        Produto baiao = produto("Baião de Dois", "Arroz, feijão-de-corda, queijo coalho e nata", "24.90");
        Produto carne = produto("Carne de Sol com Macaxeira", "Carne de sol na manteiga de garrafa", "39.90");
        Produto tapioca = produto("Tapioca de Queijo Coalho", "Goma de tapioca recheada", "14.90");
        Produto cuscuz = produto("Cuscuz Nordestino", "Cuscuz com ovo e charque", "12.50");
        Produto caju = produto("Suco de Caju", "Copo 400 ml", "8.90");
        Produto bolo = produto("Bolo de Rolo", "Fatia tradicional pernambucana", "11.00");
        Produto pastel = produto("Pastel de Camarão (edição limitada)", "Poucas unidades por dia", "18.00");

        for (Unidade u : new Unidade[]{recife, fortaleza}) {
            for (Produto p : new Produto[]{baiao, carne, tapioca, cuscuz, caju, bolo}) estoque(u, p, 50);
        }
        estoque(recife, pastel, 2); // estoque baixo: usado nos testes de conflito (409)
    }

    private void usuario(String nome, String email, String senha, Perfil perfil, boolean cliente) {
        Usuario u = new Usuario();
        u.setNome(nome); u.setEmail(email); u.setSenhaHash(encoder.encode(senha)); u.setPerfil(perfil);
        if (cliente) {
            u.setConsentimentoLgpd(true); u.setConsentimentoLgpdEm(Instant.now()); u.setFidelidadeConsentimento(true);
        }
        usuarios.save(u);
    }

    private Unidade unidade(String nome, String cidade) {
        Unidade u = new Unidade(); u.setNome(nome); u.setCidade(cidade);
        return unidades.save(u);
    }

    private Produto produto(String nome, String desc, String preco) {
        Produto p = new Produto(); p.setNome(nome); p.setDescricao(desc); p.setPreco(new BigDecimal(preco));
        return produtos.save(p);
    }

    private void estoque(Unidade u, Produto p, int qtd) {
        Estoque e = new Estoque(); e.setUnidade(u); e.setProduto(p); e.setQuantidade(qtd);
        estoques.save(e);
    }
}
