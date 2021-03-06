package com.bemyfriend.bmf.recruitment.model.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.web.multipart.MultipartFile;

import com.bemyfriend.bmf.common.code.ErrorCode;
import com.bemyfriend.bmf.common.exception.ToAlertException;
import com.bemyfriend.bmf.common.util.file.FileUtil;
import com.bemyfriend.bmf.common.util.file.FileVo;
import com.bemyfriend.bmf.common.util.paging.Paging;
import com.bemyfriend.bmf.recruitment.model.mapper.RecruMapper;
import com.bemyfriend.bmf.recruitment.model.service.RecruService;
import com.bemyfriend.bmf.recruitment.model.vo.Recruitment;

@Service
public class RecruServiceImpl implements RecruService {

	@Inject
	private RecruMapper mapper;
	
	@Autowired
	private DataSourceTransactionManager transactionManager;
	
	@Override
	public Map<String, Object> selectRecruList(int currentPage) {

		Paging paging = Paging.builder()
				.currentPage(currentPage)
				.blockCnt(10)
				.cntPerPage(16)
				.type("review")
				.total(mapper.selectContentCnt())
				.build();

		Map<String, Object> commandMap = new HashMap<String, Object>();
		
		commandMap.put("paging", paging);
		commandMap.put("recruList", mapper.selectRecruList(paging));
		
		return commandMap;
	}
	
	@Override
	public Map<String,Object> viewRecruId(int view) {

		String fileIdx = 'r'+String.valueOf(view);
		List<Map<String,FileVo>> files = mapper.selectFileWithBIdx(fileIdx);
		Map<String,Object> commandMap = new HashMap<String,Object>();
		
		commandMap.put("recru", mapper.viewRecruId(view));
		for (int i = 0; i < files.size(); i++) {
			commandMap.put("files"+i,files.get(i).get("RENAME_FILE_NAME"));	
			commandMap.put("savepath"+i,files.get(i).get("SAVE_PATH"));	
			
			if(i>4) {    //?????? 5???????????? ??????(????????? + ?????? 4???)
				break; 
			}
		}
		return commandMap;
	}
	
	@Override
	public int deleteRecru(int view) {
		System.out.println("????????? ??????");
		
		String typeIdx = "r" + String.valueOf(view); //????????? ?????? typeIdx ??????
		
		mapper.deleteRecruFile(typeIdx);
		
		return mapper.deleteRecru(view);
	}
	
	@Override
	public void updateRecru(Recruitment recruitment,List<MultipartFile> files) {
		
		FileUtil fileUtil = new FileUtil();
		
		mapper.updateRecru(recruitment);
		
		try {
			   List<FileVo> fileList = fileUtil.filesUpload(files);
			   
			   mapper.deleteRecruFile(recruitment.getTypeIdx());
			   
			   for(FileVo fileVo : fileList) {
				   fileVo.setTypeIdx(recruitment.getTypeIdx());
				  
				   mapper.insertRecruFile(fileVo);
			   }
			
		}catch(Exception e) {
			throw new ToAlertException(ErrorCode.IB01, e);
		}
	}
	
	@Override
	public void insertRecru(Recruitment recruitment, List<MultipartFile> files) {
		
		FileUtil fileUtil = new FileUtil();
		
		int newJobNo = mapper.selectRecru()+1; //????????? ?????? ??????????????? jobNo ?????? ???

		recruitment.setJobNo(newJobNo);
		TransactionStatus txStatus = transactionManager.getTransaction(new DefaultTransactionDefinition());
		try {
			mapper.insertRecru(recruitment);
		} catch(Exception e) {
			transactionManager.rollback(txStatus);
		    throw new ToAlertException(ErrorCode.IB01, e);
		}transactionManager.commit(txStatus);
		
		System.out.println(recruitment);	
		
		try {
			   List<FileVo> fileList = fileUtil.filesUpload(files);
			   
			   for(FileVo fileVo : fileList) {
				   fileVo.setTypeIdx("r"+String.valueOf(newJobNo));
				   mapper.insertRecruFile(fileVo);
			   }
			
		}catch(Exception e) {
			throw new ToAlertException(ErrorCode.IB01, e);
		}
	}
	
	

	
}
