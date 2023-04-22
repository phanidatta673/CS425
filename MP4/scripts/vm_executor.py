import os

class VMExecutor(object):
    def __init__(self, vm_id, host='fa22-cs425-50{vm_id:02}.cs.illinois.edu', username='poweiww2'):
        self.vm_id = [vm_id] if type(vm_id) == int else vm_id
        self.host = host
        self.username = username

    def run(self, cmd):
        
        if cmd is None or cmd == '':
            assert len(self.vm_id) == 1, 'Cannot connect to all machines. Please specify a machine to run the command'

            whole = f'ssh {self.username}@{self.host.format(vm_id=self.vm_id[0])}'
            print(f'Connecting: {whole}\n' + '=' * 40)
            os.system(whole)

        else:
            for vm_id in self.vm_id:
                whole = f'ssh {self.username}@{self.host.format(vm_id=vm_id)} "{cmd}"'
                print(f"Executing: {whole}\n" + '-' * 40)
                os.system(whole)
                print('=' * 40 + '\n')
