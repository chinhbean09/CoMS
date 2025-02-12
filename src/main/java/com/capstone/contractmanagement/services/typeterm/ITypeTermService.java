package com.capstone.contractmanagement.services.typeterm;

import com.capstone.contractmanagement.dtos.term.CreateTypeTermDTO;

public interface ITypeTermService {

    String createTypeTerm(CreateTypeTermDTO request);

}
