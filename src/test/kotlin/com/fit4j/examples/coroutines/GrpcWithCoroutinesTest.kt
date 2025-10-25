package com.fit4j.examples.coroutines

import com.example.coroutines.UserServiceGrpcKt
import com.example.coroutines.UsersGrpcService
import io.grpc.ServerBuilder
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import net.devh.boot.grpc.client.inject.GrpcClient
import net.devh.boot.grpc.server.serverfactory.GrpcServerConfigurer
import net.devh.boot.grpc.server.service.GrpcService
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import java.util.concurrent.Executors

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = [
    "withCoroutines=true",
    "grpc.server.inProcessName=serverWithCoroutines",
    "grpc.client.testGrpcService.address=in-process:\${grpc.server.inProcessName}"
])
class GrpcWithCoroutinesTest {

    @GrpcClient("testGrpcService")
    private lateinit var userService: UserServiceGrpcKt.UserServiceCoroutineStub
    
    @TestConfiguration
    class TestConfig {
        @Bean
        fun grpcServerConfigurer() : GrpcServerConfigurer {
            return GrpcServerConfigurer {
                t:ServerBuilder<*> -> t.executor(Executors.newFixedThreadPool(1))
            }
        }
    }

    @Test
    fun `test with coroutines`() {
        val task1 = RunnableTask(userService, "Client1", listOf(1L, 2L))
        val task2 = RunnableTask(userService, "Client2", listOf(3L, 4L))
        val task3 = RunnableTask(userService, "Client3", listOf(5L, 6L))

        val thread1 = Thread(task1)
        val thread2 = Thread(task2)
        val thread3 = Thread(task3)

        thread1.start()
        thread2.start()
        Thread.sleep(1_000)
        thread3.start()

        thread1.join()
        thread2.join()
        thread3.join()
        logger.info("${GREEN}All tasks completed, existing...$RESET")
        Thread.sleep(1_000)
    }

    class RunnableTask(private val client:UserServiceGrpcKt.UserServiceCoroutineStub,
                       private val taskName: String, private val filterIdList: List<Long>) : Runnable {
        override fun run() {
            logger.info("$GREEN$taskName running to fetch users $filterIdList...$RESET")
            val start = System.currentTimeMillis()
            val request = UsersGrpcService.GetUsersRequest.newBuilder().addAllUserId(filterIdList).build()
            val users = runBlocking {
                client.getUsers(request)
            }
            val end = System.currentTimeMillis()
            val time = (end - start) / 1000.0
            logger.info("$GREEN$taskName fetched users ${users.userList.map { it.userId }} in $time seconds$RESET")
        }
    }
}

@GrpcService
@ConditionalOnProperty(name = ["withCoroutines"], matchIfMissing = false)
class UserGrpcControllerKt(private val userService: UserService2) : UserServiceGrpcKt.UserServiceCoroutineImplBase() {
    override suspend fun getUsers(request: UsersGrpcService.GetUsersRequest): UsersGrpcService.GetUsersResponse {
        logger.info("${BLUE}GrpcController received a request to fetch users ${request.userIdList}$RESET")
        val users = userService.getUsers(request.userIdList)
        return UsersGrpcService.GetUsersResponse.newBuilder().addAllUser(users).build()
    }
}

@Service
class UserService2(private val userRepository: UserRepository2) {

    val dispatcherForIO = Executors.newFixedThreadPool(1).asCoroutineDispatcher()

    suspend fun getUsers(filterIdList:List<Long>): List<UsersGrpcService.User> {
        logger.info("${BLUE}Service fetching users $filterIdList$RESET")
        return withContext(dispatcherForIO) {
            userRepository.getUsers(filterIdList)
        }
    }
}

@Repository
class UserRepository2 {
    private var allUsers:MutableList<UsersGrpcService.User> = mutableListOf<UsersGrpcService.User>()

    init {
        for (i in 1..10) {
            val user = UsersGrpcService.User.newBuilder().setUserId(i.toLong()).build()
            allUsers.add(user)
        }
    }


    fun getUsers(filterIdList:List<Long>): List<UsersGrpcService.User> {
        logger.info("${BLUE}Repository fetching users $filterIdList$RESET")
        if(filterIdList.contains(1) || filterIdList.contains(3)) {
            logger.info("${RED}Sleeping for 10 seconds while fetching users ${filterIdList}...$RESET")
            Thread.sleep(10_000)
        }
        return allUsers.filter { filterIdList.contains(it.userId) }
    }
}



