package com.indracompany.treinamento.model.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.indracompany.treinamento.model.entity.OperacaoConta;
import com.indracompany.treinamento.model.repository.ExtratoBancarioRepository;


@Service
public class ExtratoService extends GenericCrudService<OperacaoConta, Long, ExtratoBancarioRepository>{
	
	
	@Autowired 
	private ExtratoBancarioRepository extratoBancarioRepository;
	
	
	public List<OperacaoConta> obterExtrato(String agencia, String numConta) {
		
			List<OperacaoConta> extrato = extratoBancarioRepository.findByAgenciaAndConta(agencia, numConta);
			
			return extrato;
		
		
		}
	
	
	
	

}
