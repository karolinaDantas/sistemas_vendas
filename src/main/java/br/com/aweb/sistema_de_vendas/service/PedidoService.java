package br.com.aweb.sistema_de_vendas.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.transaction.annotation.Transactional;
import br.com.aweb.sistema_de_vendas.model.Cliente;
import br.com.aweb.sistema_de_vendas.model.ItemPedido;
import br.com.aweb.sistema_de_vendas.model.Pedido;
import br.com.aweb.sistema_de_vendas.model.Produto;
import br.com.aweb.sistema_de_vendas.model.enums.StatusPedido;
import br.com.aweb.sistema_de_vendas.exception.EntidadeNaoEncontradaException;
import br.com.aweb.sistema_de_vendas.exception.RegraNegocioException;
import br.com.aweb.sistema_de_vendas.repository.PedidoRepository;
import br.com.aweb.sistema_de_vendas.repository.ProdutoRepository;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ProdutoRepository produtoRepository;

    public PedidoService(PedidoRepository pedidoRepository, ProdutoRepository produtoRepository) {
        this.pedidoRepository = pedidoRepository;
        this.produtoRepository = produtoRepository;
    }

    // CREATE - Criar novo pedido
    @Transactional
    public Pedido criarPedido(Cliente cliente) {
        Pedido pedido = new Pedido(cliente);
        return pedidoRepository.save(pedido);
    }

    // Lista todos os pedidos
    public List<Pedido> listarTodos() {
        return pedidoRepository.findAll();
    }

    // Calcular valor total do pedido
    private void calcularValorTotal(Pedido pedido) {
        BigDecimal total = BigDecimal.ZERO;

        for (ItemPedido item : pedido.getItens()) {
            BigDecimal valorItem = item.getPrecoUnitario()
                    .multiply(BigDecimal.valueOf(item.getQuantidade()));
            total = total.add(valorItem);
        }

        pedido.setValorTotal(total);
    }

    // Adicionar ITEM ao pedido
    @Transactional
    public void adicionarItem(Long pedidoId, Long produtoId, Integer quantidade) {
        Optional<Pedido> optionalPedido = pedidoRepository.findById(pedidoId);
        Optional<Produto> optionalProduto = produtoRepository.findById(produtoId);

        if (!optionalPedido.isPresent()) {
            throw new EntidadeNaoEncontradaException("Pedido não encontrado");
        }

        if (!optionalProduto.isPresent()) {
            throw new EntidadeNaoEncontradaException("Produto não encontrado");
        }

        Pedido pedido = optionalPedido.get();
        Produto produto = optionalProduto.get();

        if (pedido.getStatus() == StatusPedido.CANCELADO) {
            throw new RegraNegocioException("Não é possível adicionar itens a um pedido cancelado");
        }

        if (produto.getQuantidadeEmEstoque() < quantidade) {
            throw new RegraNegocioException("Estoque insuficiente para o produto: " + produto.getNome());
        }

        ItemPedido item = new ItemPedido(produto, quantidade);
        item.setPedido(pedido);

        pedido.getItens().add(item);

        produto.setQuantidadeEmEstoque(produto.getQuantidadeEmEstoque() - quantidade);

        calcularValorTotal(pedido);

        pedidoRepository.save(pedido);
        produtoRepository.save(produto);
    }

    // REMOVER ITEM do pedido
    @Transactional
    public void removerItem(Long pedidoId, Long itemId) {
        Optional<Pedido> optionalPedido = pedidoRepository.findById(pedidoId);

        if (!optionalPedido.isPresent()) {
            throw new EntidadeNaoEncontradaException("Pedido não encontrado");
        }
        Pedido pedido = optionalPedido.get();

        if (pedido.getStatus() == StatusPedido.CANCELADO) {
            throw new RegraNegocioException("Não é possível remover itens de um pedido cancelado");
        }

        // Busca o item no pedido pelo ID
        ItemPedido itemParaRemover = null;
        for (ItemPedido item : pedido.getItens()) {
            if (item.getId().equals(itemId)) {
                itemParaRemover = item;
                break;
            }
        }

        // Se não encontrou o item, lança exceção
        if (itemParaRemover == null) {
            throw new RegraNegocioException("Item não encontrado no pedido");
        }

        // Devolve estoque
        Produto produto = itemParaRemover.getProduto();
        produto.setQuantidadeEmEstoque(produto.getQuantidadeEmEstoque() + itemParaRemover.getQuantidade());

        pedido.getItens().remove(itemParaRemover);

        calcularValorTotal(pedido);

        pedidoRepository.save(pedido);
        produtoRepository.save(produto);
    }

    // READ - Buscar pedido por ID
    public Optional<Pedido> buscarPorId(Long id) {
        return pedidoRepository.findById(id);
    }

    // READ - Listar pedidos por status
    public List<Pedido> listarPorStatus(StatusPedido status) {
        return pedidoRepository.findByStatus(status);
    }

    // FINALIZAR Pedido
    @Transactional
    public void finalizarPedido(Long id) {
        Optional<Pedido> optionalPedido = pedidoRepository.findById(id);
        if (!optionalPedido.isPresent()) {
            throw new EntidadeNaoEncontradaException("Pedido não encontrado");
        }
        Pedido pedido = optionalPedido.get();
        if (pedido.getStatus() != StatusPedido.ATIVO) {
            throw new RegraNegocioException("Apenas pedidos ativos podem ser finalizados");
        }
        pedido.setStatus(StatusPedido.ENTREGUE);
        pedidoRepository.save(pedido);
    }

    // CANCELAR Pedido
    @Transactional
    public void cancelarPedido(Long id) {
        Optional<Pedido> optionalPedido = pedidoRepository.findById(id);
        if (!optionalPedido.isPresent()) {
            throw new EntidadeNaoEncontradaException("Pedido não encontrado");
        }
        Pedido pedido = optionalPedido.get();
        if (pedido.getStatus() == StatusPedido.CANCELADO) {
            throw new RegraNegocioException("Pedido já está cancelado");
        }
        
        // Devolve o estoque
        for (ItemPedido item : pedido.getItens()) {
            Produto produto = item.getProduto();
            produto.setQuantidadeEmEstoque(produto.getQuantidadeEmEstoque() + item.getQuantidade());
            produtoRepository.save(produto);
        }
        
        pedido.setStatus(StatusPedido.CANCELADO);
        pedidoRepository.save(pedido);
    }
}
