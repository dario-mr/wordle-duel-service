package com.dariom.wds.persistence.repository.jpa;

import com.dariom.wds.domain.DictionaryWordType;
import com.dariom.wds.domain.Language;
import com.dariom.wds.persistence.entity.DictionaryWordEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DictionaryWordJpaRepository extends JpaRepository<DictionaryWordEntity, Long> {

  List<DictionaryWordEntity> findByLanguageAndType(Language language, DictionaryWordType type);
}
