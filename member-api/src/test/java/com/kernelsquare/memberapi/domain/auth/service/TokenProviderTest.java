package com.kernelsquare.memberapi.domain.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kernelsquare.core.type.AuthorityType;
import com.kernelsquare.domainmysql.domain.auth.info.AuthInfo;
import com.kernelsquare.domainmysql.domain.authority.entity.Authority;
import com.kernelsquare.domainmysql.domain.level.entity.Level;
import com.kernelsquare.domainmysql.domain.member.entity.Member;
import com.kernelsquare.domainmysql.domain.member.info.MemberInfo;
import com.kernelsquare.domainmysql.domain.member_authority.entity.MemberAuthority;
import com.kernelsquare.memberapi.domain.auth.dto.MemberAdapter;
import com.kernelsquare.memberapi.domain.auth.dto.MemberAdaptorInstance;
import com.kernelsquare.memberapi.domain.auth.dto.TokenRequest;
import com.kernelsquare.memberapi.domain.auth.dto.TokenResponse;
import com.kernelsquare.memberapi.domain.auth.entity.RefreshToken;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("토큰 생성 서비스 테스트")
@ExtendWith(MockitoExtension.class)
public class TokenProviderTest {
	@InjectMocks
	private TokenProvider tokenProvider;
	@Spy
	private RedisTemplate<Long, RefreshToken> redisTemplate = spy(RedisTemplate.class);

	@Mock
	private MemberDetailService memberDetailService;

	private String secret = "dGVzdF9zZWNyZXRfdGVzdF9zZWNyZXRfdGVzdF9zZWNyZXRfdGVzdF9zZWNyZXRfdGVzdF9zZWNyZXRfdGVzdF9zZWNyZXRfdGVzdF9zZWNyZXRfdGVzdF9zZWNyZXRfdGVzdF9zZWNyZXRfdGVzdF9zZWNyZXRf";

	private ObjectMapper objectMapper = new ObjectMapper();

	@BeforeEach
	public void setUp() {
		byte[] keyBytes = Decoders.BASE64.decode(secret);
		ReflectionTestUtils.setField(tokenProvider, "key", Keys.hmacShaKeyFor(keyBytes));

		objectMapper.registerModule(new JavaTimeModule());
		objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

		String classPropertyTypeName = "RefreshToken.class";
		GenericJackson2JsonRedisSerializer jsonRedisSerializer =
			new GenericJackson2JsonRedisSerializer(classPropertyTypeName);
		jsonRedisSerializer.configure(objectMapper -> objectMapper
			.registerModule(new JavaTimeModule()));

		LettuceConnectionFactory lettuceConnectionFactory = new LettuceConnectionFactory();
		lettuceConnectionFactory.afterPropertiesSet();

		redisTemplate.setConnectionFactory(lettuceConnectionFactory);
		redisTemplate.setKeySerializer(new JdkSerializationRedisSerializer());
		redisTemplate.setValueSerializer(jsonRedisSerializer);
		redisTemplate.afterPropertiesSet();
	}

	@Test
	@DisplayName("jwt 생성 테스트")
	void testCreateToken() throws Exception {
		//given
		Level level = Level.builder()
			.id(1L)
			.name(1L)
			.imageUrl("s3:whatever")
			.levelUpperLimit(500L)
			.build();

		Member member = Member.builder()
			.id(1L)
			.nickname("machine")
			.email("awdag@nsavasc.om")
			.password("hashed")
			.experience(1200L)
			.introduction("basfas")
			.authorities(List.of(
				MemberAuthority.builder()
					.member(Member.builder().build())
					.authority(Authority.builder().authorityType(AuthorityType.ROLE_USER).build())
					.build()))
			.imageUrl("agawsc")
			.level(level)
			.build();

		MemberAdapter memberAdapter = new MemberAdapter(MemberAdaptorInstance.of(member));

		doReturn(memberAdapter)
			.when(memberDetailService)
			.loadUserByUsername(anyString());

		//when
		AuthInfo.LoginInfo loginInfo = tokenProvider.createToken(MemberInfo.from(member));

		RefreshToken createdRefreshToken = objectMapper.readValue(Decoders.BASE64
			.decode(loginInfo.refreshToken()), RefreshToken.class);

		//then
		assertThat(createdRefreshToken.getMemberId()).isEqualTo(member.getId());

		//verify
		verify(memberDetailService, only()).loadUserByUsername(anyString());
		verify(memberDetailService, times(1)).loadUserByUsername(anyString());
	}

	@Test
	@DisplayName("로그 아웃 테스트")
	void testLogout() throws Exception {
		//given
		Long testMemberId = 1L;
		String accessToken = "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIyIiwiYXV0aCI6IlJPTEVfVVNFUiIsImV4cCI6MTkwMDAwMDAwMH0.eHSaEoaFl9Rb7R6YxwyKiACOObHN0XjiNO7T7i1KpkiqVbgz9hQr5EPq5DuRliA_UlsBeIvfU8UHPG7xhwdcRg";
		String refreshTokenString = "eyJtZW1iZXJJZCI6MiwicmVmcmVzaFRva2VuIjoiMjYzOGUxYjQ3MmI2NDRkNTk4YzY1NGNlZWFlN2FhOTAiLCJjcmVhdGVkRGF0ZSI6IjIwMjQtMDEtMTBUMjE6MDA6MzIuNDYxOCIsImV4cGlyYXRpb25EYXRlIjoiMjAyNC0wMS0yNFQyMTowMDozMi40NjE3NiJ9";

		TokenRequest tokenRequest = TokenRequest.builder()
			.refreshToken(refreshTokenString)
			.accessToken(accessToken)
			.build();

		ValueOperations<Long, RefreshToken> longRefreshTokenValueOperations = mock(ValueOperations.class);

		RedisOperations<Long, RefreshToken> operations = mock(RedisOperations.class);

		doReturn(longRefreshTokenValueOperations)
			.when(redisTemplate)
			.opsForValue();

		doReturn(operations)
			.when(longRefreshTokenValueOperations)
			.getOperations();

		doReturn(Boolean.TRUE)
			.when(operations)
			.delete(anyLong());

		//when
		tokenProvider.logout(tokenRequest);
		Boolean delete = redisTemplate.opsForValue().getOperations().delete(testMemberId);

		//then
		assertThat(delete).isTrue();

		//verify
		verify(redisTemplate, times(2)).opsForValue();
		verify(redisTemplate.opsForValue(), times(2)).getOperations();
		verify(redisTemplate.opsForValue().getOperations(), times(2)).delete(anyLong());
	}

	/**
	 * AccessToken은 만료 시간을 들고 있어서 이 코드 대로면 시간이 지나면 항상 테스트가 터져버림..
	 * 따라서 임의로 AccessToken 만료 기한을 2030년까지로 설정하여 일단 테스트가 통과 될 수 있도록 지정 해놓음
	 */
	@Test
	@DisplayName("토큰 재발급 테스트")
	void testReissueToken() throws Exception {
		//given
		String accessToken = "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIyIiwiYXV0aCI6IlJPTEVfVVNFUiIsImV4cCI6MTkwMDAwMDAwMH0.eHSaEoaFl9Rb7R6YxwyKiACOObHN0XjiNO7T7i1KpkiqVbgz9hQr5EPq5DuRliA_UlsBeIvfU8UHPG7xhwdcRg";

		String refreshTokenString = "eyJtZW1iZXJJZCI6MiwicmVmcmVzaFRva2VuIjoiMjYzOGUxYjQ3MmI2NDRkNTk4YzY1NGNlZWFlN2FhOTAiLCJjcmVhdGVkRGF0ZSI6IjIwMjQtMDEtMTBUMjE6MDA6MzIuNDYxOCIsImV4cGlyYXRpb25EYXRlIjoiMjEyNC0xMi0yNFQyMTowMDozMi40NjE3NiJ9";

		TokenRequest tokenRequest = TokenRequest
			.builder()
			.accessToken(accessToken)
			.refreshToken(refreshTokenString)
			.build();

		RefreshToken refreshToken = objectMapper.readValue(Decoders.BASE64
			.decode(refreshTokenString), RefreshToken.class);

		ValueOperations<Long, RefreshToken> longRefreshTokenValueOperations = mock(ValueOperations.class);

		doReturn(longRefreshTokenValueOperations)
			.when(redisTemplate)
			.opsForValue();

		doReturn(refreshToken)
			.when(longRefreshTokenValueOperations)
			.get(anyLong());

		//when
		TokenResponse tokenResponse = tokenProvider.reissueToken(tokenRequest);
		RefreshToken createdRefreshToken = objectMapper.readValue(Decoders.BASE64.decode(tokenResponse.refreshToken()),
			RefreshToken.class);

		//then
		assertThat(createdRefreshToken.getMemberId()).isEqualTo(refreshToken.getMemberId());

		//verify
		verify(redisTemplate, times(2)).opsForValue();
		verify(redisTemplate.opsForValue(), times(1)).get(anyLong());
	}
}
