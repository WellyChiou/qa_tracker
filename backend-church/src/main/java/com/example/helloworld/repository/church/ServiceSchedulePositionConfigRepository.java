package com.example.helloworld.repository.church;

import com.example.helloworld.entity.church.ServiceSchedulePositionConfig;
import com.example.helloworld.entity.church.ServiceScheduleDate;
import com.example.helloworld.entity.church.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ServiceSchedulePositionConfigRepository extends JpaRepository<ServiceSchedulePositionConfig, Long> {
    Optional<ServiceSchedulePositionConfig> findByServiceScheduleDateAndPosition(ServiceScheduleDate serviceScheduleDate, Position position);
}

