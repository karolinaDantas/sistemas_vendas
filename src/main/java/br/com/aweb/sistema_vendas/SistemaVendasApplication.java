package br.com.aweb.sistema_vendas;

import br.com.aweb.sistema_vendas.model.Usuario;
import br.com.aweb.sistema_vendas.model.UsuarioRole;
import br.com.aweb.sistema_vendas.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class SistemaVendasApplication {

    public static void main(String[] args) {
        SpringApplication.run(SistemaVendasApplication.class, args);
    }

    // Cria utilizadores de teste automaticamente ao iniciar a aplicação
    @Bean
    public CommandLineRunner initData(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            // Cria Administrador se não existir
            if (usuarioRepository.findByEmailIgnoreCase("admin@sistema.com").isEmpty()) {
                Usuario admin = new Usuario();
                admin.setNome("Administrador Master");
                admin.setEmail("admin@sistema.com");
                admin.setSenha(passwordEncoder.encode("123456")); // Senha criptografada
                admin.setRole(UsuarioRole.ADMIN);
                usuarioRepository.save(admin);
                System.out.println("-> Utilizador ADMIN criado: admin@sistema.com / 123456");
            }

            // Cria Utilizador Padrão se não existir
            if (usuarioRepository.findByEmailIgnoreCase("padrao@sistema.com").isEmpty()) {
                Usuario padrao = new Usuario();
                padrao.setNome("Utilizador Padrão");
                padrao.setEmail("padrao@sistema.com");
                padrao.setSenha(passwordEncoder.encode("123456")); // Senha criptografada
                padrao.setRole(UsuarioRole.PADRAO);
                usuarioRepository.save(padrao);
                System.out.println("-> Utilizador PADRAO criado: padrao@sistema.com / 123456");
            }
        };
    }
}