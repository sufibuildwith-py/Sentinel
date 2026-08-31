package com.sentinel.revenue.replay;
import org.springframework.context.annotation.*;
import java.time.Clock;
@Configuration
public class ReplayConfiguration { @Bean public Clock replayClock(){ return Clock.systemUTC(); } }
