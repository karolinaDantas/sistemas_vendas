package br.com.aweb.sistema_de_vendas.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

import br.com.aweb.sistema_de_vendas.exception.EntidadeNaoEncontradaException;
import br.com.aweb.sistema_de_vendas.model.Pedido;
import br.com.aweb.sistema_de_vendas.model.enums.StatusPedido;
import br.com.aweb.sistema_de_vendas.service.ClienteService;
import br.com.aweb.sistema_de_vendas.service.PedidoService;
import br.com.aweb.sistema_de_vendas.service.ProdutoService;

@Controller
@RequestMapping("/pedidos")
public class PedidoController {
    
    private final PedidoService pedidoService;
    private final ClienteService clienteService;
    private final ProdutoService produtoService;

    public PedidoController(PedidoService pedidoService, ClienteService clienteService, ProdutoService produtoService) {
        this.pedidoService = pedidoService;
        this.clienteService = clienteService;
        this.produtoService = produtoService;
    }

    // Listar todos os pedidos
    @GetMapping
    public ModelAndView listarPedidos() {
        return new ModelAndView("pedido/list", Map.of("pedidos", pedidoService.listarTodos()));
    }

    @GetMapping("/novo")
    public ModelAndView novoPedidoForm() {
        return new ModelAndView("pedido/form",
            Map.of(
                "pedido", new Pedido(),
                "clientes", clienteService.listarTodos(),
                "produtos", produtoService.listarTodos()
            )
        );
    }

    // Criar novo pedido
    @PostMapping("/novo")
    public String criarPedido(@RequestParam Long clienteId) {
        var optionalCliente = clienteService.buscarPorId(clienteId);
        if(!optionalCliente.isPresent()) {
            throw new EntidadeNaoEncontradaException("Cliente não encontrado");
        }

        Pedido pedido = pedidoService.criarPedido(optionalCliente.get());
        return "redirect:/pedidos/edit/" + pedido.getId();
    }

    // Formulário de edição de pedido
    @GetMapping("/edit/{id}")
    public ModelAndView editarPedidoForm(@PathVariable Long id) {
        var optionalPedido =  pedidoService.buscarPorId(id);
        if(!optionalPedido.isPresent()) {
            throw new EntidadeNaoEncontradaException("Pedido não encontrado");
        }

        Pedido pedido = optionalPedido.get();

        // Não permite editar pedidos cancelados
        if(pedido.getStatus() == StatusPedido.CANCELADO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não é possível editar um pedido cancelado");
        }

        return new ModelAndView("pedido/edit",
            Map.of(
                "pedido", pedido,
                "produtos", produtoService.listarTodos()
            )
        );
    }

    // Adicionar item ao pedido
    @PostMapping("/{pedidoId}/adicionar-item")
    public String adicionarItem(@PathVariable Long pedidoId, @RequestParam Long produtoId, @RequestParam Integer quantidade) {
       try {
            pedidoService.adicionarItem(pedidoId, produtoId, quantidade);
            return "redirect:/pedidos/edit/" + pedidoId;
       } catch(IllegalArgumentException | IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());

       }
    }

    // Finalizar pedido
    @PostMapping("/{id}/finalizar")
    public String finalizarPedido(@PathVariable  Long id) {
        try {
            pedidoService.finalizarPedido(id);
            return "redirect:/pedidos";
        } catch(IllegalArgumentException | IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }


    // Cancelar Pedido - Formulário de Confirmação
    @GetMapping("/cancelar/{id}")
    public ModelAndView cancelarPedidoForm(@PathVariable Long id) {
        var optionalPedido = pedidoService.buscarPorId(id);
        if(!optionalPedido.isPresent()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return new ModelAndView("pedido/cancelar", Map.of("pedido", optionalPedido.get()));
    }

    // Cancelar Pedido - Ação
    @PostMapping("/cancelar/{id}")
    public String cancelarPedido(@PathVariable Long id) {
        try {
            pedidoService.cancelarPedido(id);
            return "redirect:/pedidos";
        } catch(IllegalArgumentException | IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}
