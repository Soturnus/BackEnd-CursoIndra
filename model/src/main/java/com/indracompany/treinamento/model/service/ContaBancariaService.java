package com.indracompany.treinamento.model.service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.indracompany.treinamento.exception.AplicacaoException;
import com.indracompany.treinamento.exception.ExceptionValidacoes;
import com.indracompany.treinamento.model.dto.ClienteDTO;
import com.indracompany.treinamento.model.dto.TransferenciaBancariaDTO;
import com.indracompany.treinamento.model.entity.Cliente;
import com.indracompany.treinamento.model.entity.ContaBancaria;
import com.indracompany.treinamento.model.entity.OperacaoConta;
import com.indracompany.treinamento.model.repository.ContaBancariaRepository;

@Service
public class ContaBancariaService extends GenericCrudService<ContaBancaria, Long, ContaBancariaRepository> {

	@Autowired
	private ClienteService clienteService;

	@Autowired
	private ContaBancariaRepository contaBancariaRepository;

	@Autowired
	private ExtratoService extrato;

	Date data = new Date();
	SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
	

	public double consultarSaldo(String agencia, String numeroConta) {
		ContaBancaria c = this.consultarConta(agencia, numeroConta);
		return c.getSaldo();
	}

	public ContaBancaria consultarConta(String agencia, String numeroConta) {
		ContaBancaria c = contaBancariaRepository.findByAgenciaAndNumero(agencia, numeroConta);
		if (c == null) {
			throw new AplicacaoException(ExceptionValidacoes.ERRO_CONTA_INVALIDA);
		}
		return c;
	}

	public List<ContaBancaria> obterContas(String cpf) {
		ClienteDTO dto = clienteService.buscarClientePorCpf(cpf);
		Cliente cliente = clienteService.buscar(dto.getId());
		List<ContaBancaria> contasDoCliente = contaBancariaRepository.findByCliente(cliente);
		return contasDoCliente;
	}

	public void depositar(String agencia, String numeroConta, double valor, boolean ehTransf) {
		ContaBancaria conta = this.consultarConta(agencia, numeroConta);
		conta.setSaldo(conta.getSaldo() + valor);
		
		if(!ehTransf) {
			fmt.setTimeZone(TimeZone.getTimeZone("GMT-3:00"));
			String dataHora = fmt.format(data);
			OperacaoConta ext = new OperacaoConta();
			ext.setAgencia(agencia);
			ext.setConta(numeroConta);
			ext.setCliente(conta.getCliente());
			ext.setOperacao("depositar");
			ext.setObservacao("Deposito efetuado");
			ext.setValor(valor);
			ext.setSaldo(conta.getSaldo());
			ext.setDataHora(dataHora);
			extrato.salvar(ext);
		}
		
		super.salvar(conta);
	}

	public void sacar(String agencia, String numeroConta, double valor, boolean ehTransf) {
		ContaBancaria conta = this.consultarConta(agencia, numeroConta);

		if (conta.getSaldo() < valor) {
			throw new AplicacaoException(ExceptionValidacoes.ERRO_SALDO_INEXISTENTE);
		}

		conta.setSaldo(conta.getSaldo() - valor);
		

		if(!ehTransf) {
			fmt.setTimeZone(TimeZone.getTimeZone("GMT-3:00"));
			String dataHora = fmt.format(data);
			OperacaoConta ext = new OperacaoConta();
			ext.setAgencia(agencia);
			ext.setConta(numeroConta);
			ext.setCliente(conta.getCliente());
			ext.setOperacao("saque");
			ext.setObservacao("Saque efetuado");
			ext.setValor(valor);
			ext.setSaldo(conta.getSaldo());
			ext.setDataHora(dataHora);
			extrato.salvar(ext);
		}
		
		super.salvar(conta);
	}

	@Transactional(rollbackOn = Exception.class)
	public void transferir(TransferenciaBancariaDTO dto) {
		if (dto.getAgenciaOrigem().equals(dto.getAgenciaDestino())
				&& dto.getNumeroContaOrigem().equals(dto.getNumeroContaDestino())) {
			throw new AplicacaoException(ExceptionValidacoes.ERRO_CONTA_INVALIDA);
		}
		this.sacar(dto.getAgenciaOrigem(), dto.getNumeroContaOrigem(), dto.getValor(), true);
		this.depositar(dto.getAgenciaDestino(), dto.getNumeroContaDestino(), dto.getValor(), true);

		ContaBancaria conta = this.consultarConta(dto.getAgenciaOrigem(), dto.getNumeroContaOrigem());
		ContaBancaria contaDestino = this.consultarConta(dto.getAgenciaDestino(), dto.getNumeroContaDestino());

		fmt.setTimeZone(TimeZone.getTimeZone("GMT-3:00"));
		String dataHora = fmt.format(data);

		OperacaoConta operacaoOrigem = new OperacaoConta();

		operacaoOrigem.setAgencia(dto.getAgenciaOrigem());
		operacaoOrigem.setConta(dto.getNumeroContaOrigem());
		operacaoOrigem.setCliente(conta.getCliente());
		operacaoOrigem.setOperacao("transferencia");
		operacaoOrigem.setObservacao("Transferencia efetuada com sucesso para:  Agencia " + dto.getAgenciaDestino() + "Conta " + dto.getNumeroContaDestino());
		operacaoOrigem.setValor(dto.getValor());
		operacaoOrigem.setSaldo(conta.getSaldo());
		operacaoOrigem.setContaOrigem(dto.getAgenciaOrigem() + " " + dto.getNumeroContaOrigem());
		operacaoOrigem.setContaDestino(dto.getAgenciaDestino() + " " + dto.getNumeroContaDestino());
		operacaoOrigem.setDataHora(dataHora);
		extrato.salvar(operacaoOrigem);

		OperacaoConta operacaoDestino = new OperacaoConta();

		operacaoDestino.setAgencia(dto.getAgenciaDestino());
		operacaoDestino.setConta(dto.getNumeroContaDestino());
		operacaoDestino.setCliente(contaDestino.getCliente());
		operacaoDestino.setOperacao("transferencia");
		operacaoDestino.setObservacao("Transferencia efetuada com sucesso para: Agencia " + dto.getAgenciaDestino() + " Conta " + dto.getNumeroContaDestino());
		operacaoDestino.setValor(dto.getValor());
		operacaoDestino.setSaldo(contaDestino.getSaldo());
		operacaoDestino.setContaOrigem(dto.getAgenciaOrigem() + " " + dto.getNumeroContaOrigem());
		operacaoDestino.setContaDestino(dto.getAgenciaDestino() + " " + dto.getNumeroContaDestino());
		operacaoDestino.setDataHora(dataHora);
		extrato.salvar(operacaoDestino);

	}

}
