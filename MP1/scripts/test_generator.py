import argparse
import numpy as np

patterns = {
    'rare': 'rare',
    'mid': 'mid',
    'freq': 'freq'
}

n_occurs = (5, 295, 700)

def main(args):
    arr = np.arange(1000, dtype=int)
    np.random.shuffle(arr)
    print(arr)
    result_s = ''
    for i in range(1000):
        if arr[i] < 5:
            result_s += patterns['rare']
        elif arr[i] < 300:
            result_s += patterns['mid']
        else:
            result_s += patterns['freq']

        if args.one:
            result_s += '1'
        elif args.some:
            result_s += '2'
        result_s += '\n'
    print(result_s)


    file_name = 'my.log'
    with open(file_name, 'w') as f:
        f.write(result_s)

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Test case generator.')
    parser.add_argument('--one', action='store_true',
                        help='generate for "one machine" test case')
    parser.add_argument('--some', action='store_true',
                        help='generate for "some machine" test case')
    args = parser.parse_args()
    assert args.one != args.some or args.one is False, 'Cannot set both flags at the same time!'
    print(args)
    main(args)