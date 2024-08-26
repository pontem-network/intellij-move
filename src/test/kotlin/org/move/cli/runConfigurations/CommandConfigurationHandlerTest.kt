package org.move.cli.runConfigurations

import org.move.cli.moveProjectsService
import org.move.cli.runConfigurations.aptos.CommandConfigurationHandler
import org.move.cli.runConfigurations.aptos.run.RunCommandConfigurationHandler
import org.move.cli.runConfigurations.aptos.view.ViewCommandConfigurationHandler
import org.move.utils.tests.MvProjectTestBase
import org.move.utils.tests.TreeBuilder

class CommandConfigurationHandlerTest : MvProjectTestBase() {
    fun `test parse run command with explicitly defined function`() =
        testCommand(
            {
                _aptos_config_yaml(
                    """---
profiles:
    another:
        private_key: "0x4543a4d8eb859b4054b8508aaaa6edb0e9327336e53a8f0134133c4bac2a1354"
        public_key: "0x58af52ff0fbe1e4dd8eb7024b9ef713c68f91d565138b024d035771970dcf97e"
        account: 7f906a4591cfdddcc2c1efb06835ef3faa1feab27d799c24156d5462926fc415
        rest_url: "https://fullnode.testnet.aptoslabs.com"
"""
                )
                namedMoveToml("MyPackage")
                sources {
                    main(
                        """
                module 0x1::m {
                    public entry fun main(account: &signer, balance: u64) {/*caret*/}
                }
            """
                    )
                }
            },
            "move run --profile another --function-id 0x1::m::main --args u64:100",
            RunCommandConfigurationHandler(),
            expectedProfile = "another"
        )

    fun `test parse view command with explicitly defined function`() =
        testCommand(
            {
                _aptos_config_yaml(
                    """---
profiles:
    default:
        private_key: "0x4543a4d8eb859b4054b8508aaaa6edb0e9327336e53a8f0134133c4bac2a1354"
        public_key: "0x58af52ff0fbe1e4dd8eb7024b9ef713c68f91d565138b024d035771970dcf97e"
        account: 7f906a4591cfdddcc2c1efb06835ef3faa1feab27d799c24156d5462926fc415
        rest_url: "https://fullnode.testnet.aptoslabs.com"
"""
                )
                namedMoveToml("MyPackage")
                sources {
                    main(
                        """
                module 0x1::m {
                    #[view]
                    public fun get_balance(balance: u64) {/*caret*/}
                }
            """
                    )
                }
            },
            "move view --profile default --function-id 0x1::m::get_balance --args u64:100",
            ViewCommandConfigurationHandler(),
        )

    private fun testCommand(
        builder: TreeBuilder,
        command: String,
        handler: CommandConfigurationHandler,
        expectedProfile: String = "default",
    ) {
        testProject(builder)

        val moveProject = project.moveProjectsService.allProjects.first()
        val (profile, functionCall) = handler.parseTransactionCommand(moveProject, command).unwrap()

        check(profile == expectedProfile) { "Unexpected profile $profile" }

        val generatedCommand = handler.generateCommand(functionCall, profile).unwrap()
        check(command == generatedCommand) {
            "Commands are not equal. \n" +
                    "Original: $command\n" +
                    "Generated: $generatedCommand"
        }
    }
}
